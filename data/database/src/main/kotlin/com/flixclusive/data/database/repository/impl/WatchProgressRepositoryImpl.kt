package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.EpisodeProgressDao
import com.flixclusive.core.database.dao.MovieProgressDao
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
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
    private val appDispatchers: AppDispatchers
) : WatchProgressRepository {
    override fun getAllAsFlow(ownerId: Int): Flow<List<WatchProgressWithMetadata>> {
        return movieProgressDao.getAllAsFlow(ownerId)
            .combine(episodeProgressDao.getAllAsFlow(ownerId)) { movies, episodes ->
                (movies + episodes).sortedByDescending { it.watchData.watchedAt }
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

    override fun getAsFlow(id: Long, type: FilmType): Flow<WatchProgressWithMetadata?> {
        return when (type) {
            FilmType.MOVIE -> movieProgressDao.getAsFlow(id)
            FilmType.TV_SHOW -> episodeProgressDao.getAsFlow(id)
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
                    .sortedByDescending { it.watchData.watchedAt }
            }.distinctUntilChanged()
        }
    }

    override suspend fun insert(item: WatchProgress, film: Film?): Long {
        return withContext(appDispatchers.io) {
            val dbFilm = film?.toDBFilm()

            when (item) {
                is MovieProgress -> movieProgressDao.insert(item = item, film = dbFilm)
                is EpisodeProgress -> episodeProgressDao.insert(item = item, film = dbFilm)
            }
        }
    }

    override suspend fun delete(item: Long, type: FilmType) {
        withContext(appDispatchers.io) {
            when (type) {
                FilmType.MOVIE -> movieProgressDao.delete(item)
                FilmType.TV_SHOW -> episodeProgressDao.delete(item)
            }
        }
    }

    override suspend fun removeAll(ownerId: Int) {
        withContext(appDispatchers.io) {
            movieProgressDao.deleteAll(ownerId)
            episodeProgressDao.deleteAll(ownerId)
        }
    }
}
