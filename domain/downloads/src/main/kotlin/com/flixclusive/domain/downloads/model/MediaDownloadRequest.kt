package com.flixclusive.domain.downloads.model

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle

data class MediaDownloadRequest(
    val media: MediaMetadata,
    val episode: Episode?,
    val streams: List<Stream>,
    val subtitle: Subtitle?,
)
