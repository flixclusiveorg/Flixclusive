package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.dao.library.LibraryListItemDao
import com.flixclusive.core.database.dao.watched.EpisodeProgressDao
import com.flixclusive.core.database.dao.watched.MovieProgressDao
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.util.FilmType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class WatchProgressRepositoryImpl @Inject constructor(
    private val movieProgressDao: MovieProgressDao,
    private val episodeProgressDao: EpisodeProgressDao,
    private val libraryListItemDao: LibraryListItemDao,
    private val libraryListDao: LibraryListDao,
    private val appDispatchers: AppDispatchers
) : WatchProgressRepository {
    override fun getAllAsFlow(ownerId: Int, sort: LibrarySort): Flow<List<WatchProgressWithMetadata>> {
        val column = when (sort) {
            is LibrarySort.Added -> "createdAt"
            is LibrarySort.Modified -> "updatedAt"
            is LibrarySort.Name -> "createdAt" // Name sorting will be done in-memory after merging the lists
        }

        return combine(
            flow = movieProgressDao.getAllAsFlow(
                ownerId = ownerId,
                column = column,
                ascending = sort.ascending
            ),
            flow2 = episodeProgressDao.getAllAsFlow(
                ownerId = ownerId,
                column = column,
                ascending = sort.ascending
            ),
        ) { movies, episodes ->
            mergeSortedLists(
                a = movies,
                b = episodes,
                comparator = compareBy {
                    when (sort) {
                        is LibrarySort.Added -> it.watchData.createdAt
                        is LibrarySort.Modified -> it.watchData.updatedAt
                        is LibrarySort.Name -> it.film.title
                    }
                }
            )
        }
        .distinctUntilChanged()
    }

    override suspend fun get(id: Long, type: FilmType): WatchProgressWithMetadata? {
        return withContext(appDispatchers.io) {
            when (type) {
                FilmType.MOVIE -> movieProgressDao.get(id)
                FilmType.TV_SHOW -> episodeProgressDao.get(id)
            }
        }
    }

    override suspend fun get(
        id: String,
        ownerId: Int,
        type: FilmType,
    ): WatchProgressWithMetadata? {
        return withContext(appDispatchers.io) {
            when (type) {
                FilmType.MOVIE -> movieProgressDao.get(id, ownerId)
                FilmType.TV_SHOW -> episodeProgressDao.get(id, ownerId)
            }
        }
    }

    override suspend fun getSeasonProgress(
        tvShowId: String,
        seasonNumber: Int,
        ownerId: Int
    ): List<EpisodeProgress> {
        return withContext(appDispatchers.io) {
            episodeProgressDao.getSeasonProgress(
                filmId = tvShowId,
                season = seasonNumber,
                ownerId = ownerId
            )
        }
    }

    override fun getSeasonProgressAsFlow(
        tvShowId: String,
        seasonNumber: Int,
        ownerId: Int
    ): Flow<List<EpisodeProgress>> = episodeProgressDao.getSeasonProgressAsFlow(
        filmId = tvShowId,
        season = seasonNumber,
        ownerId = ownerId
    )

    override fun getAsFlow(id: Long, type: FilmType): Flow<WatchProgressWithMetadata?> {
        return when (type) {
            FilmType.MOVIE -> movieProgressDao.getAsFlow(id)
            FilmType.TV_SHOW -> episodeProgressDao.getAsFlow(id)
        }
    }

    override fun getAsFlow(
        id: String,
        ownerId: Int,
        type: FilmType
    ): Flow<WatchProgressWithMetadata?> {
        return when (type) {
            FilmType.MOVIE -> movieProgressDao.getAsFlow(id, ownerId)
            FilmType.TV_SHOW -> episodeProgressDao.getAsFlow(id, ownerId)
        }
    }

    override suspend fun getRandoms(ownerId: Int, count: Int): Flow<List<WatchProgressWithMetadata>> {
        return withContext(appDispatchers.io) {
            combine(
                movieProgressDao.getRandoms(ownerId = ownerId, count = count),
                episodeProgressDao.getRandoms(ownerId = ownerId, count = count)
            ) { movies, episodes ->
                (movies + episodes)
                    .shuffled()
                    .sortedByDescending { it.watchData.createdAt }
            }.distinctUntilChanged()
        }
    }

    override suspend fun insert(item: WatchProgress, film: Film?): Long {
        return withContext(appDispatchers.io) {
            val dbFilm = film?.toDBFilm()
            val watchedList = libraryListDao.getWatchedList(item.ownerId)
            libraryListItemDao.insert(
                film = film,
                item = LibraryListItem(
                    filmId = item.filmId,
                    listId = watchedList.id,
                ),
            )

            when (item) {
                is MovieProgress -> movieProgressDao.insert(item = item, film = dbFilm)
                is EpisodeProgress -> episodeProgressDao.insert(item = item, film = dbFilm)
            }
        }
    }

    override suspend fun removeAll(ownerId: Int) {
        withContext(appDispatchers.io) {
            movieProgressDao.deleteAll(ownerId)
            episodeProgressDao.deleteAll(ownerId)
        }
    }

    override suspend fun delete(item: Long, type: FilmType) {
        withContext(appDispatchers.io) {
            val ownerId = when (type) {
                FilmType.MOVIE -> movieProgressDao.get(item)?.watchData?.ownerId
                FilmType.TV_SHOW -> episodeProgressDao.get(item)?.watchData?.ownerId
            }

            if (ownerId == null) {
                errorLog("WatchProgressRepository.delete - no watch progress found for id: $item and type: $type")
                return@withContext
            }

            var canDeleteOnLibrary = type == FilmType.MOVIE
            val watchedList = libraryListDao.getWatchedList(ownerId)
            if (type == FilmType.TV_SHOW) {
                val episodeProgress = episodeProgressDao.get(item)
                if (episodeProgress != null) {
                    val season = episodeProgressDao.getSeasonProgress(
                        filmId = episodeProgress.filmId,
                        season = episodeProgress.watchData.seasonNumber,
                        ownerId = ownerId
                    )

                    canDeleteOnLibrary = season.size == 1
                }
            }

            if (canDeleteOnLibrary) {
                val filmId = when (type) {
                    FilmType.MOVIE -> movieProgressDao.get(item)?.filmId
                    FilmType.TV_SHOW -> episodeProgressDao.get(item)?.filmId
                }

                if (filmId == null) {
                    errorLog("WatchProgressRepository.delete - no watch progress found for id: $item and type: $type")
                    return@withContext
                }

                libraryListItemDao.deleteByListIdAndFilmId(
                    filmId = filmId,
                    listId = watchedList.id
                )
            }

            when (type) {
                FilmType.MOVIE -> movieProgressDao.delete(item)
                FilmType.TV_SHOW -> episodeProgressDao.delete(item)
            }
        }
    }

    private fun mergeSortedLists(
        a: List<WatchProgressWithMetadata>,
        b: List<WatchProgressWithMetadata>,
        comparator: Comparator<in WatchProgressWithMetadata>
    ): List<WatchProgressWithMetadata> {
        val merged = mutableListOf<WatchProgressWithMetadata>()
        var i = 0
        var j = 0

        while (i < a.size && j < b.size) {
            if (comparator.compare(a[i], b[j]) <= 0) {
                merged.add(a[i++])
            } else {
                merged.add(b[j++])
            }
        }

        merged.addAll(a.subList(i, a.size))
        merged.addAll(b.subList(j, b.size))

        return merged
    }
}
