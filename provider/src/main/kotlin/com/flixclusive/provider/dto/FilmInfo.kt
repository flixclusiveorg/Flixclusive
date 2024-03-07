package com.flixclusive.provider.dto

data class FilmInfo(
    val id: String,
    val title: String,
    val yearReleased: String = "",
    val seasons: Int? = null,
    val episodes: Int? = null
)
