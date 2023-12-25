package com.flixclusive.providers.models.common

data class EmbedData(
    val servers: List<VideoDataServer>,
    val subtitles: List<Subtitle> = emptyList()
)