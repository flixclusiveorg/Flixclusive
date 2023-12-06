package com.flixclusive.providers.models.common

import java.io.Serializable

//data class MediaServer(val serverName: String) {
//    override fun toString(): String {
//        return serverName
//    }
//
//    companion object {
//        fun String.toMediaServer() = MediaServer(serverName = this)
//    }
//}
data class VideoDataServer(
    val serverName: String,
    val serverUrl: String,
    val isEmbed: Boolean = false,
) : Serializable