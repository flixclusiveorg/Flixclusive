package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.media.MediaMetadata

interface ViewMediaAction {
    fun openMediaScreen(media: MediaMetadata)
}
