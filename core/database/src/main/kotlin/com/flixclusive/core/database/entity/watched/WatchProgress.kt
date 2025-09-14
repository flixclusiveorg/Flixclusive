package com.flixclusive.core.database.entity.watched

import java.util.Date

/**
 * Represents a progress item in the watch history.
 *
 * This can be either a [MovieProgress] or an [EpisodeProgress].
 *
 * It contains the owner ID, progress in seconds, status of the watch,
 * duration of the film in seconds, and the date when it was watched.
 * */
sealed interface WatchProgress {
    val id: Long
    val ownerId: Int
    val progress: Long
    val duration: Long
    val status: WatchStatus
    val watchedAt: Date

    /**
     * Determines whether the watch progress is considered finished.
     *
     * A watch is considered finished if:
     * - The status is [WatchStatus.COMPLETED].
     * - The progress is at least 95% of the total duration.
     * */
    val isFinished: Boolean
        get() {
            if (status == WatchStatus.COMPLETED) return true
            if (duration <= 0) return false

            val percentage = (progress.toDouble() / duration.toDouble()) * 100
            return percentage >= WATCH_COMPLETED_THRESHOLD
        }
    val isWatching get() = status == WatchStatus.WATCHING
    val isRewatching get() = status == WatchStatus.REWATCHING

    /**
     * Determines whether the current playback time is less than a minute.
     * */
    fun isLessThanAMinute(): Boolean = progress <= 60_000L

    // TODO: Move this to a utility class on `player` module
    companion object {
        private const val WATCH_COMPLETED_THRESHOLD = 95 // 95% of the total duration
    }
}
