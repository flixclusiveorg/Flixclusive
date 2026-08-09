package com.flixclusive.data.downloads.probe

interface LinkProbe {
    suspend fun probe(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): LinkProbeResult
}
