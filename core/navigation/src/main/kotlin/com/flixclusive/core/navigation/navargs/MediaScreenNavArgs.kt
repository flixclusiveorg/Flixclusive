package com.flixclusive.core.navigation.navargs

import com.flixclusive.model.media.MediaMetadata

open class MediaScreenNavArgs(
    val media: MediaMetadata,
    val isTogglingLibrary: Boolean,
)
