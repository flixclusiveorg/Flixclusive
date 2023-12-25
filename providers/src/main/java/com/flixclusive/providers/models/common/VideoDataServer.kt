package com.flixclusive.providers.models.common

import java.io.Serializable

data class VideoDataServer(
    val serverName: String,
    val serverUrl: String,
) : Serializable