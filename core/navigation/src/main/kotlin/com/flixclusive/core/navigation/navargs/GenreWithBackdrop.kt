package com.flixclusive.core.navigation.navargs

import com.flixclusive.model.film.Genre
import java.io.Serializable

data class GenreWithBackdrop(
    val id: Int,
    val name: String,
    val posterPath: String,
    val mediaType: String
) : Serializable {
    fun toGenre() = Genre(
        id = id,
        name = name,
        mediaType = mediaType
    )

    companion object {
        fun Genre.toGenreWithBackdrop() = GenreWithBackdrop(
            id = id,
            name = name,
            posterPath = "",
            mediaType = mediaType ?: ""
        )
    }
}
