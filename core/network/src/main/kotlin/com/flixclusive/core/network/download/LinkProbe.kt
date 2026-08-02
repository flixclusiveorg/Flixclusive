package com.flixclusive.core.network.download

interface LinkProbe {
    suspend fun probe(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): LinkProbeResult
}
