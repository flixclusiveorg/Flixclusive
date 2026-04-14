package com.flixclusive.data.backup.create.impl

import com.flixclusive.core.database.dao.watched.EpisodeProgressDao
import com.flixclusive.core.database.dao.watched.MovieProgressDao
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.create.BackupCreator
import com.flixclusive.data.backup.model.BackupWatchEpisodeProgress
import com.flixclusive.data.backup.model.BackupWatchMovieProgress
import com.flixclusive.data.backup.model.BackupWatchProgress
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class WatchProgressBackupCreator @Inject constructor(
    private val episodeProgressDao: EpisodeProgressDao,
    private val movieProgressDao: MovieProgressDao,
    private val userSessionDataStore: UserSessionDataStore
) : BackupCreator<BackupWatchProgress> {
    override suspend fun invoke(): Result<List<BackupWatchProgress>> {
        return runCatching {
            val userId = userSessionDataStore.currentUserId.filterNotNull().first()

            val episodes = episodeProgressDao.getAll(userId)
            val movies = movieProgressDao.getAll(userId)

            val combined = episodes + movies

            combined.map { it.toBackupItem() }
        }
    }

    private fun MovieProgressWithMetadata.toBackupItem(): BackupWatchMovieProgress {
        return BackupWatchMovieProgress(
            filmId = filmId,
            progress = watchData.progress,
            status = watchData.status,
            duration = watchData.duration,
            createdAt = watchData.createdAt.time,
            updatedAt = watchData.updatedAt.time
        )
    }

    private fun EpisodeProgressWithMetadata.toBackupItem(): BackupWatchEpisodeProgress {
        return BackupWatchEpisodeProgress(
            filmId = filmId,
            progress = watchData.progress,
            status = watchData.status,
            duration = watchData.duration,
            episodeNumber = watchData.episodeNumber,
            seasonNumber = watchData.seasonNumber,
            createdAt = watchData.createdAt.time,
            updatedAt = watchData.updatedAt.time,
        )
    }

    private fun WatchProgressWithMetadata.toBackupItem(): BackupWatchProgress {
        return when (this) {
            is EpisodeProgressWithMetadata -> toBackupItem()
            is MovieProgressWithMetadata -> toBackupItem()
        }
    }
}
