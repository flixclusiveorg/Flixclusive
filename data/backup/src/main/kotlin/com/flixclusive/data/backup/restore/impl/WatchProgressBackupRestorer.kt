package com.flixclusive.data.backup.restore.impl

import com.flixclusive.core.database.dao.library.LibraryListDao
import com.flixclusive.core.database.dao.library.LibraryListItemDao
import com.flixclusive.core.database.dao.watched.EpisodeProgressDao
import com.flixclusive.core.database.dao.watched.MovieProgressDao
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupWatchEpisodeProgress
import com.flixclusive.data.backup.model.BackupWatchMovieProgress
import com.flixclusive.data.backup.model.BackupWatchProgress
import com.flixclusive.data.backup.restore.BackupRestorer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

internal class WatchProgressBackupRestorer @Inject constructor(
    private val episodeProgressDao: EpisodeProgressDao,
    private val movieProgressDao: MovieProgressDao,
    private val libraryListItemDao: LibraryListItemDao,
    private val libraryListDao: LibraryListDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupRestorer<BackupWatchProgress> {
    override suspend fun invoke(items: List<BackupWatchProgress>): Result<Unit> {
        return runCatching {
            val ownerId = userSessionDataStore.currentUserId.filterNotNull().first()

            val watchedList = libraryListDao.getWatchedList(ownerId)

            items.forEach { progress ->
                val existing = libraryListItemDao.getByListIdAndFilmId(
                    listId = watchedList.id,
                    filmId = progress.filmId,
                )

                val listItem = existing?.item?.copy(
                    createdAt = Date(progress.createdAt),
                    updatedAt = Date(progress.updatedAt),
                ) ?: LibraryListItem(
                    filmId = progress.filmId,
                    listId = watchedList.id,
                    createdAt = Date(progress.createdAt),
                    updatedAt = Date(progress.updatedAt),
                )

                libraryListItemDao.insertItem(listItem)

                when (progress) {
                    is BackupWatchMovieProgress -> movieProgressDao.insertProgress(
                        progress.toMovieProgress(ownerId)
                    )
                    is BackupWatchEpisodeProgress -> episodeProgressDao.insertProgress(
                        progress.toEpisodeProgress(ownerId)
                    )
                }
            }

            if (items.isNotEmpty()) {
                val latestUpdatedAt = items.maxOf { it.updatedAt }
                libraryListDao.update(watchedList.copy(updatedAt = Date(latestUpdatedAt)))
            }
        }
    }

    private fun BackupWatchMovieProgress.toMovieProgress(ownerId: String): MovieProgress {
        return MovieProgress(
            filmId = filmId,
            ownerId = ownerId,
            progress = progress,
            status = toActualStatus(),
            duration = duration,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt),
        )
    }

    private fun BackupWatchEpisodeProgress.toEpisodeProgress(ownerId: String): EpisodeProgress {
        return EpisodeProgress(
            filmId = filmId,
            ownerId = ownerId,
            progress = progress,
            status = toActualStatus(),
            duration = duration,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt),
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
        )
    }

    private fun BackupWatchProgress.toActualStatus(): WatchStatus {
        val isCompletedByThreshold = status == WatchStatus.WATCHING &&
            duration > 0L &&
            (progress.toDouble() / duration.toDouble()) * 100 >= WATCH_COMPLETED_THRESHOLD

        return if (isCompletedByThreshold) WatchStatus.COMPLETED else status
    }

    private companion object {
        private const val WATCH_COMPLETED_THRESHOLD = 95
    }
}
