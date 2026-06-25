package com.flixclusive.feature.mobile.player

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import kotlinx.serialization.Serializable

/**
 * Navigation arguments for the PlayerScreen.
 *
 * @param media The media metadata to be played.
 * @param episode The episode to be played (if the media is a TV show).
 * */
data class PlayerScreenNavArgs(
    val media: MediaMetadata,
    val episode: Episode?,
    val initialStreamUrl: String?,
    val initialCacheId: String?,
    val initialHeaders: PlayerScreenInitialHeader?,
)

@Serializable
data class PlayerScreenInitialHeader(
    val headers: Map<String, String>
)
