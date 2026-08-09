package com.flixclusive.data.downloads.probe

data class LinkProbeResult(
    val isReachable: Boolean,
    val contentLength: Long?,
    val bytesPerSecond: Long?,
    val isHls: Boolean = false,
)
