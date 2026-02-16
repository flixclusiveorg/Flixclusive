package com.flixclusive.feature.mobile.player.util

internal enum class UiMode {
    NONE,
    SUBS,
    SERVERS,
    SUBS_SYNC,
    EPISODES,
    RESIZE,
    PLAYBACK_SPEED;

    val isNone get() = this == NONE
    val isSubs get() = this == SUBS
    val isServers get() = this == SERVERS
    val isSubsSync get() = this == SUBS_SYNC
    val isEpisodes get() = this == EPISODES
    val isResize get() = this == RESIZE
    val isPlaybackSpeed get() = this == PLAYBACK_SPEED
}
