package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.media.MediaMetadata

interface NavigateToMediaScreen {
    fun navigateToMediaScreen(
        media: MediaMetadata,
        isTogglingLibrary: Boolean = false,
    )
}
