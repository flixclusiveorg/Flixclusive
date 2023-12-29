package com.flixclusive.providers.models.common

data class EmbedData(
    val servers: List<SourceLink>,
    val subtitles: List<Subtitle> = emptyList()
)