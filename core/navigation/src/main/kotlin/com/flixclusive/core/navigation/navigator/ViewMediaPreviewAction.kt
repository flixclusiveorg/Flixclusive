package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.media.MediaMetadata

interface ViewMediaPreviewAction {
    fun previewMedia(media: MediaMetadata)
}
