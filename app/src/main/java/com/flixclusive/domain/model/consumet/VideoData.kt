package com.flixclusive.domain.model.consumet

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class VideoData(
    val title: String? = null,
    val mediaId: String? = null,
    val episodeId: String? = null,
    val headers: Headers = Headers(),
    val sources: List<Source> = emptyList(),
    val subtitles: List<Subtitle> = emptyList(),
    val servers: List<VideoDataServer>? = null
) : Serializable

data class Headers(
    @SerializedName("Referer") val referer: String = ""
) : Serializable

data class Source(
    val url: String = "",
    val quality: String = "",
    val isM3U8: Boolean = false
) : Serializable

data class Subtitle(
    val url: String = "",
    val lang: String = ""
) : Serializable
