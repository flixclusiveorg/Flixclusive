package com.flixclusive.extractor.upcloud.dto

import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.provider.SubtitleSource
import com.google.gson.annotations.SerializedName

data class UpCloudEmbedData(
    val sources: String,
    val tracks: List<UpCloudEmbedSubtitleData>,
    val encrypted: Boolean,
    val server: Int
) {
    data class UpCloudEmbedSubtitleData(
        @SerializedName("file") val url: String,
        @SerializedName("label") val lang: String,
        val kind: String
    )

    companion object {
        fun UpCloudEmbedSubtitleData.toSubtitle() = Subtitle(
            url = url,
            language = lang,
            type = SubtitleSource.ONLINE
        )
    }
}