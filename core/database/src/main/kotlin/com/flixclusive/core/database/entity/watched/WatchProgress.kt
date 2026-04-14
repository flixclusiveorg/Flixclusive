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
    val ownerId: String
    val progress: Long
    val duration: Long
    val status: WatchStatus
    val createdAt: Date
    val updatedAt: Date

    val isAboveThreshold: Boolean get() {
        val percentage = (progress.toDouble() / duration.toDouble()) * 100
        return percentage >= WATCH_COMPLETED_THRESHOLD
    }
    val isCompleted: Boolean get() = status == WatchStatus.COMPLETED
    val isWatching get() = status == WatchStatus.WATCHING

    /**
     * Determines whether the current playback time is less than a minute.
     * */
    fun isLessThanAMinute(): Boolean = progress <= 60_000L

    companion object {
        private const val WATCH_COMPLETED_THRESHOLD = 95 // 95% of the total duration
    }
}
