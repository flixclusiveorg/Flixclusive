package com.flixclusive_provider.models.extractors.vidcloud

import com.google.gson.annotations.SerializedName


data class VidCloudEmbedData(
    val sources: String,
    val tracks: List<VidCloudEmbedSubtitleData>,
    val encrypted: Boolean,
    val server: Int
) {
    data class VidCloudEmbedSubtitleData(
        @SerializedName("file") val url: String,
        @SerializedName("label") val lang: String,
        val kind: String
    )
}