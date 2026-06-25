package com.flixclusive.data.database.repository.impl

import com.flixclusive.core.common.dispatchers.AppDispatchers
import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.dao.library.LibraryListItemDao
import com.flixclusive.core.database.dao.watched.EpisodeProgressDao
import com.flixclusive.core.database.dao.watched.MovieProgressDao
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.util.log.errorLog
import com.flixclusive.data.database.repository.LibrarySort
import com.flixclusive.data.database.repository.WatchProgressRepository
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

internal class WatchProgressRepositoryImpl @Inject constructor(
    private val movieProgressDao: MovieProgressDao,
    private val episodeProgressDao: EpisodeProgressDao,
    private val libraryListItemDao: LibraryListItemDao,
    private val libraryListDao: LibraryListDao,
    private val appDispatchers: AppDispatchers
) : WatchProgressRepository {
    override fun getAllAsFlow(ownerId: String, sort: LibrarySort): Flow<List<WatchProgressWithMetadata>> {
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
            val comparator = compareBy<WatchProgressWithMetadata> {
                when (sort) {
                    is LibrarySort.Added -> it.watchData.createdAt
                    is LibrarySort.Modified -> it.watchData.updatedAt
                    is LibrarySort.Name -> it.media.title
                }
            }.let {
                if (sort.ascending) it else it.reversed()
            }

            mergeSortedLists(
                a = movies,
                b = episodes,
                comparator = comparator
            )
        }.distinctUntilChanged()
    }

    override suspend fun get(id: Long, type: MediaType): WatchProgressWithMetadata? {
        return withContext(appDispatchers.io) {
            when (type) {
                MediaType.MOVIE -> movieProgressDao.get(id)
                MediaType.SHOW -> episodeProgressDao.get(id)
            }
        }
    }

    override suspend fun get(
        id: String,
        ownerId: String,
        type: MediaType,
    ): WatchProgressWithMetadata? {
        return withContext(appDispatchers.io) {
            when (type) {
                MediaType.MOVIE -> movieProgressDao.get(id, ownerId)
                MediaType.SHOW -> episodeProgressDao.get(id, ownerId)
            }
        }
    }

    override suspend fun getSeasonProgress(
        tvShowId: String,
        seasonNumber: Int,
        ownerId: String
    ): List<EpisodeProgress> {
        return withContext(appDispatchers.io) {
            episodeProgressDao.getSeasonProgress(
                mediaId = tvShowId,
                season = seasonNumber,
                ownerId = ownerId
            )
        }
    }

    override suspend fun getEpisodeProgress(
        tvShowId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        ownerId: String
    ): EpisodeProgress? {
        return withContext(appDispatchers.io) {
            episodeProgressDao.getEpisodeProgress(
                mediaId = tvShowId,
                season = seasonNumber,
                episode = episodeNumber,
                ownerId = ownerId
            )
        }
    }

    override fun getSeasonProgressAsFlow(
        tvShowId: String,
        seasonNumber: Int,
        ownerId: String
    ): Flow<List<EpisodeProgress>> = episodeProgressDao.getSeasonProgressAsFlow(
        mediaId = tvShowId,
        season = seasonNumber,
        ownerId = ownerId
    )

    override fun getAsFlow(id: Long, type: MediaType): Flow<WatchProgressWithMetadata?> {
        return when (type) {
            MediaType.MOVIE -> movieProgressDao.getAsFlow(id)
            MediaType.SHOW -> episodeProgressDao.getAsFlow(id)
        }
    }

    override fun getAsFlow(
        id: String,
        ownerId: String,
        type: MediaType
    ): Flow<WatchProgressWithMetadata?> {
        return when (type) {
            MediaType.MOVIE -> movieProgressDao.getAsFlow(id, ownerId)
            MediaType.SHOW -> episodeProgressDao.getAsFlow(id, ownerId)
        }
    }

    override suspend fun insert(item: WatchProgress, media: MediaMetadata?): Long {
        return withContext(appDispatchers.io) {
            val dbMedia = media?.toDBMedia()
            val watchedList = libraryListDao.getWatchedList(item.ownerId)
            val existingListItem = libraryListItemDao.getByListIdAndMediaId(
                listId = watchedList.id,
                mediaId = item.mediaId
            )
            libraryListDao.update(watchedList.copy(updatedAt = Date()))
            libraryListItemDao.insert(
                media = media,
                item = existingListItem?.item?.copy(
                    updatedAt = Date()
                ) ?: LibraryListItem(
                    mediaId = item.mediaId,
                    listId = watchedList.id,
                ),
            )

            val actualStatus = when {
                item.isWatching && item.isAboveThreshold -> WatchStatus.COMPLETED
                else -> item.status
            }

            when (item) {
                is MovieProgress -> {
                    movieProgressDao.insert(
                        item = item.copy(status = actualStatus),
                        media = dbMedia?.copy(updatedAt = Date())
                    )
                }

                is EpisodeProgress -> {
                    episodeProgressDao.insert(
                        item = item.copy(status = actualStatus),
                        media = dbMedia?.copy(updatedAt = Date())
                    )
                }
            }
        }
    }

    override suspend fun deleteAll(ownerId: String) {
        withContext(appDispatchers.io) {
            val watchedList = libraryListDao.getWatchedList(ownerId)
            libraryListDao.update(watchedList.copy(updatedAt = Date()))

            movieProgressDao.deleteAll(ownerId)
            episodeProgressDao.deleteAll(ownerId)
        }
    }

    override suspend fun delete(item: Long, type: MediaType) {
        withContext(appDispatchers.io) {
            val ownerId = when (type) {
                MediaType.MOVIE -> movieProgressDao.get(item)?.watchData?.ownerId
                MediaType.SHOW -> episodeProgressDao.get(item)?.watchData?.ownerId
            }

            if (ownerId == null) {
                errorLog("WatchProgressRepository.delete - no watch progress found for id: $item and type: $type")
                return@withContext
            }

            var canDeleteOnLibrary = type == MediaType.MOVIE
            val watchedList = libraryListDao.getWatchedList(ownerId)
            libraryListDao.update(watchedList.copy(updatedAt = Date()))

            if (type == MediaType.SHOW) {
                val episodeProgress = episodeProgressDao.get(item)
                if (episodeProgress != null) {
                    val season = episodeProgressDao.getSeasonProgress(
                        mediaId = episodeProgress.mediaId,
                        season = episodeProgress.watchData.seasonNumber,
                        ownerId = ownerId
                    )

                    canDeleteOnLibrary = season.size == 1
                }
            }

            if (canDeleteOnLibrary) {
                val mediaId = when (type) {
                    MediaType.MOVIE -> movieProgressDao.get(item)?.mediaId
                    MediaType.SHOW -> episodeProgressDao.get(item)?.mediaId
                }

                if (mediaId == null) {
                    errorLog("WatchProgressRepository.delete - no watch progress found for id: $item and type: $type")
                    return@withContext
                }

                libraryListItemDao.deleteByListIdAndMediaId(
                    mediaId = mediaId,
                    listId = watchedList.id
                )
            }

            when (type) {
                MediaType.MOVIE -> movieProgressDao.delete(item)
                MediaType.SHOW -> episodeProgressDao.delete(item)
            }
        }
    }

    private fun mergeSortedLists(
        a: List<WatchProgressWithMetadata>,
        b: List<WatchProgressWithMetadata>,
        comparator: Comparator<in WatchProgressWithMetadata>
    ): List<WatchProgressWithMetadata> {
        return (a + b).sortedWith(comparator)
    }
}
