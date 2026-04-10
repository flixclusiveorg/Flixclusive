package com.flixclusive.data.backup.validate.impl

import com.flixclusive.core.database.dao.watched.EpisodeProgressDao
import com.flixclusive.core.database.dao.watched.MovieProgressDao
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.data.backup.model.BackupWatchEpisodeProgress
import com.flixclusive.data.backup.model.BackupWatchMovieProgress
import com.flixclusive.data.backup.model.BackupWatchProgress
import com.flixclusive.data.backup.validate.BackupValidationMode
import com.flixclusive.data.backup.validate.BackupValidator
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class WatchProgressBackupValidator @Inject constructor(
    private val episodeProgressDao: EpisodeProgressDao,
    private val movieProgressDao: MovieProgressDao,
    private val userSessionDataStore: UserSessionDataStore,
) : BackupValidator<BackupWatchProgress> {
    override suspend fun invoke(
        backup: List<BackupWatchProgress>,
        mode: BackupValidationMode,
    ): Result<Set<String>> {
        return runCatching {
            val ownerId = userSessionDataStore.currentUserId.filterNotNull().first()

            when (mode) {
                BackupValidationMode.CREATE -> validateCreate(ownerId = ownerId, backup = backup)
                BackupValidationMode.RESTORE -> validateRestore(ownerId = ownerId, backup = backup)
            }
        }
    }

    private fun BackupWatchEpisodeProgress.key(): String {
        return "${filmId}:${seasonNumber}:${episodeNumber}"
    }

    private fun EpisodeProgressWithMetadata.key(): String {
        return "${watchData.filmId}:${watchData.seasonNumber}:${watchData.episodeNumber}"
    }

    private fun BackupWatchProgress.toRestoredStatus(): WatchStatus {
        val isCompletedByThreshold = status == WatchStatus.WATCHING &&
            duration > 0L &&
            (progress.toDouble() / duration.toDouble()) * 100 >= WATCH_COMPLETED_THRESHOLD

        return if (isCompletedByThreshold) WatchStatus.COMPLETED else status
    }

    private fun MovieProgressWithMetadata.matches(backup: BackupWatchMovieProgress): Boolean {
        return watchData.progress == backup.progress &&
            watchData.status == backup.status &&
            watchData.duration == backup.duration &&
            watchData.createdAt.time == backup.createdAt &&
            watchData.updatedAt.time == backup.updatedAt
    }

    private fun EpisodeProgressWithMetadata.matches(backup: BackupWatchEpisodeProgress): Boolean {
        return watchData.progress == backup.progress &&
            watchData.status == backup.status &&
            watchData.duration == backup.duration &&
            watchData.createdAt.time == backup.createdAt &&
            watchData.updatedAt.time == backup.updatedAt
    }

    private suspend fun validateCreate(ownerId: Int, backup: List<BackupWatchProgress>): Set<String> {
        val expectedMovies = movieProgressDao.getAll(ownerId)
        val expectedEpisodes = episodeProgressDao.getAll(ownerId)

        val backupMoviesByFilmId = backup
            .filterIsInstance<BackupWatchMovieProgress>()
            .associateBy { it.filmId }

        val backupEpisodesByKey = backup
            .filterIsInstance<BackupWatchEpisodeProgress>()
            .associateBy { it.key() }

        val missing = linkedSetOf<String>()
        expectedMovies.forEach { expected ->
            val actual = backupMoviesByFilmId[expected.watchData.filmId]
            if (actual == null || !expected.matches(actual)) {
                missing.add(expected.watchData.filmId)
            }
        }

        expectedEpisodes.forEach { expected ->
            val key = expected.key()
            val actual = backupEpisodesByKey[key]
            if (actual == null || !expected.matches(actual)) {
                missing.add(key)
            }
        }

        return missing
    }

    private suspend fun validateRestore(ownerId: Int, backup: List<BackupWatchProgress>): Set<String> {
        val expectedMovies = backup.filterIsInstance<BackupWatchMovieProgress>()
        val expectedEpisodes = backup.filterIsInstance<BackupWatchEpisodeProgress>()

        if (expectedMovies.isEmpty() && expectedEpisodes.isEmpty()) return emptySet()

        val actualMoviesByFilmId = movieProgressDao.getAll(ownerId).associateBy { it.watchData.filmId }
        val actualEpisodesByKey = episodeProgressDao.getAll(ownerId).associateBy { it.key() }

        val missing = linkedSetOf<String>()
        expectedMovies.forEach { expected ->
            val actual = actualMoviesByFilmId[expected.filmId]
            if (actual == null) {
                missing.add(expected.filmId)
                return@forEach
            }

            val matches = actual.watchData.progress == expected.progress &&
                actual.watchData.status == expected.toRestoredStatus() &&
                actual.watchData.duration == expected.duration &&
                actual.watchData.createdAt.time == expected.createdAt &&
                actual.watchData.updatedAt.time == expected.updatedAt

            if (!matches) missing.add(expected.filmId)
        }

        expectedEpisodes.forEach { expected ->
            val key = expected.key()
            val actual = actualEpisodesByKey[key]
            if (actual == null) {
                missing.add(key)
                return@forEach
            }

            val matches = actual.watchData.progress == expected.progress &&
                actual.watchData.status == expected.toRestoredStatus() &&
                actual.watchData.duration == expected.duration &&
                actual.watchData.createdAt.time == expected.createdAt &&
                actual.watchData.updatedAt.time == expected.updatedAt

            if (!matches) missing.add(key)
        }

        return missing
    }

    private companion object {
        private const val WATCH_COMPLETED_THRESHOLD = 95
    }
}
