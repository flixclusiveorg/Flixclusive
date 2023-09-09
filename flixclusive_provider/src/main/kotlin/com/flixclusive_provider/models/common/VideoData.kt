package com.flixclusive_provider.models.common

import java.io.Serializable

data class VideoData(
    val title: String? = null,
    val mediaId: String = "",
    val episodeId: String = "",
    val source: String = "",
    val subtitles: List<Subtitle> = emptyList(),
    val servers: List<VideoDataServer>? = null
) : Serializable

data class Subtitle(
    val url: String = "",
    val lang: String = ""
) : Serializable
