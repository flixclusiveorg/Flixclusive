package com.flixclusive.domain.downloads.model

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

data class MediaDownloadRequest(
    val media: MediaMetadata,
    val episode: Episode?,
    val ownerId: String,
)
