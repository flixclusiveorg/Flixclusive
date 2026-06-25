package com.flixclusive.core.presentation.player.model.track

import androidx.compose.runtime.Immutable

/**
 * Immutable data class representing available servers.
 * This is exposed to the UI layer for server selection.
 */
@Immutable
data class PlayerServer(
    override val label: String,
    override val isDead: Boolean,
    val url: String,
    val headers: Map<String, String>?,
    val source: TrackSource
) : PlayerTrack
