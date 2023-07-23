package com.flixclusive.domain.model.consumet

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class VideoDataServer(
    @SerializedName("name") val serverName: String,
    @SerializedName("url") val serverUrl: String,
) : Serializable