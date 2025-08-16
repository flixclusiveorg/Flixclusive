package com.flixclusive.domain.database.usecase

import com.flixclusive.core.database.entity.watched.WatchProgress

/**
 * Use case for setting the watch time of a movie or TV show episode.
 * */
interface SetWatchProgressUseCase {
    /**
     * Sets the watch time progress for a movie or tv show episode based on the
     * current watch time and total duration.
     *
     * @param watchProgress The [WatchProgress] to update.
     * */
    suspend operator fun invoke(watchProgress: WatchProgress)
}
