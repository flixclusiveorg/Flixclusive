package com.flixclusive.provider.flixhq.dto

data class FlixHQInitialSourceData(
    val type: String,
    val link: String,
    val sources: List<Any>,
    val tracks: List<Any>,
    val title: String
)