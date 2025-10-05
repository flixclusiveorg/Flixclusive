package com.flixclusive.core.presentation.player.model

import androidx.compose.runtime.Immutable

/**
 * Holds all media-related data for a specific provider's content.
 * This includes the servers, subtitles, media source, and playback state.
 */
@Immutable
class MediaItemKey(
    val providerId: String,
    val filmId: String,
    val episodeId: String?
) {
    override fun toString(): String {
        val filmKey = when(episodeId) {
            null -> "${filmId}_movie"
            else -> "${filmId}_episode_$episodeId"
        }

        return "${providerId}_$filmKey"
    }
}
