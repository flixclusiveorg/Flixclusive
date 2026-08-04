package com.flixclusive.feature.mobile.player

import com.flixclusive.core.navigation.navargs.PlaybackRequest

/**
 * Navigation arguments for the PlayerScreen — a single declared [PlaybackRequest] rather than a
 * pile of nullable fields whose validity depended on one another.
 * */
data class PlayerScreenNavArgs(
    val request: PlaybackRequest,
)
