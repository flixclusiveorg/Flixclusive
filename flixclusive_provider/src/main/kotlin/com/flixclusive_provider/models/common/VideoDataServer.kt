package com.flixclusive_provider.models.common

import java.io.Serializable

enum class MediaServer(val serverName: String) {
    UpCloud(serverName = "upcloud"),
    VidCloud(serverName = "vidcloud");
    //MixDrop(serverName = "mixdrop"); temporarily unavailable

    companion object {
        fun String.toMediaServer() = when(this) {
            "upcloud" -> UpCloud
            "vidcloud" -> VidCloud
            //"mixdrop" -> MixDrop
            else -> throw IllegalStateException("Invalid Media Server!")
        }
    }
}
data class VideoDataServer(
    val serverName: String,
    val serverUrl: String,
) : Serializable