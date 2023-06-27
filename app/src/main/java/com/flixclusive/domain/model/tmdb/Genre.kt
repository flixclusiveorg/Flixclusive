package com.flixclusive.domain.model.tmdb

import com.flixclusive.R
import kotlinx.serialization.Serializable

@Serializable
data class Genre(
    val id: Int,
    val name: String,
    val labelId: Int? = null,
    val mediaType: String? = null,
    val posterPath: String? = null
) : java.io.Serializable

val GENRES_LIST = listOf(
    Genre(28, "Action", labelId = R.string.action_label),
    Genre(12, "Adventure", labelId = R.string.adventure_label),
    Genre(16, "Animation", labelId = R.string.animation_label),
    Genre(35, "Comedy", labelId = R.string.comedy_label),
    Genre(80, "Crime", labelId = R.string.crime_label),
    Genre(99, "Documentary", labelId = R.string.documentary_label),
    Genre(18, "Drama", labelId = R.string.drama_label),
    Genre(10751, "Family", labelId = R.string.family_label),
    Genre(14, "Fantasy", labelId = R.string.fantasy_label),
    Genre(36, "History", labelId = R.string.history_label),
    Genre(27, "Horror", labelId = R.string.horror_label),
    Genre(10402, "Music", labelId = R.string.music_label),
    Genre(9648, "Mystery", labelId = R.string.mystery_label),
    Genre(10749, "Romance", labelId = R.string.romance_label),
    Genre(878, "Science Fiction", labelId = R.string.science_fiction_label),
    Genre(10770, "TV Movie", labelId = R.string.tv_movie_label),
    Genre(53, "Thriller", labelId = R.string.thriller_label),
    Genre(10752, "War", labelId = R.string.war_label),
    Genre(37, "Western", labelId = R.string.western_label)
)
