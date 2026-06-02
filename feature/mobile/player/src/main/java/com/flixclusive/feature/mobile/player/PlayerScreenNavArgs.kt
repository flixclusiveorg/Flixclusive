package com.flixclusive.feature.mobile.player

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

/**
 * Navigation arguments for the PlayerScreen.
 *
 * @param media The media metadata to be played.
 * @param episode The episode to be played (if the media is a TV show).
 * */
data class PlayerScreenNavArgs(
    val media: MediaMetadata,
    val streamUrl: String,
    val episode: Episode?,
    val cacheId: String?,
)
