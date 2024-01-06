package com.flixclusive.extractor.upcloud.dto

import com.flixclusive.model.provider.Subtitle
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
        fun UpCloudEmbedSubtitleData.toSubtitle(customName: String? = null) = Subtitle(
            url = url,
            lang = customName + lang
        )
    }
}