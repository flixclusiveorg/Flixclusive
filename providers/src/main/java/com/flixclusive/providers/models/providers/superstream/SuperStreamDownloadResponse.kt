package com.flixclusive.providers.models.providers.superstream

import com.google.gson.annotations.SerializedName

internal data class SuperStreamDownloadResponse(
    val code: String? = null,
    val msg: String? = null,
    val data: DataResponse = DataResponse()
) {
    data class DataResponse(
        val seconds: Int? = null,
        val quality: List<String> = listOf(),
        val list: List<DownloadItem> = listOf()
    )

    data class DownloadItem(
        val path: String? = null,
        val quality: String? = null,
        @SerializedName("real_quality") val realQuality: String? = null,
        val format: String? = null,
        val fid: Int? = null,
        val size: String? = null,
        val width: Int? = null,
        val height: Int? = null
    )
}