package com.flixclusive.domain.database.usecase

import com.flixclusive.model.media.MediaMetadata

/**
 * This use case allows adding or removing a movie or TV show episode from the watch progress.
 * */
interface ToggleWatchProgressStatusUseCase {
    /**
     * Toggles the watch progress status of a movie or TV show episode.
     *
     * @param media The media (movie or TV show episode) to toggle the watch progress status for.
     * */
    suspend operator fun invoke(media: MediaMetadata)
}
