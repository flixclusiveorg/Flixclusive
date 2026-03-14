package com.flixclusive.core.database.entity.watched

import java.util.Date

/**
 * Common interface for watch progress items.
 *
 * Implementations: [MovieProgress], [EpisodeProgress].
 * */
sealed interface WatchProgress {
    val id: Long
    val filmId: String
    val ownerId: Int
    val progress: Long
    val duration: Long
    val status: WatchStatus
    val createdAt: Date
    val updatedAt: Date

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

    /**
     * Determines whether the current playback time is less than a minute.
     * */
    fun isLessThanAMinute(): Boolean = progress <= 60_000L

    companion object {
        private const val WATCH_COMPLETED_THRESHOLD = 95 // 95% of the total duration
    }
}
