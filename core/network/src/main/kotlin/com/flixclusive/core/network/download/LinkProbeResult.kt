package com.flixclusive.core.network.download

data class LinkProbeResult(
    val isReachable: Boolean,
    val contentLength: Long?,
    val bytesPerSecond: Long?,
    val isHls: Boolean = false,
)
