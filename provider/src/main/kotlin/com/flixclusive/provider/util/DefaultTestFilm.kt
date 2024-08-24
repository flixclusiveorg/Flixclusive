package com.flixclusive.provider.util

import com.flixclusive.model.tmdb.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.tmdb.Movie
import com.flixclusive.provider.ProviderApi

/**
 * 
 * The default test film used by any provider.
 * This film is [The Godfather (1972)](https://www.themoviedb.org/movie/238-the-godfather).
 * 
 * @see ProviderApi.testFilm
 * */
val defaultTestFilm = Movie(
    id = "tt0068646",
    tmdbId = 238,
    imdbId = "tt0068646",
    title = "The Godfather",
    rating = 8.691,
    year = 1972,
    homePage = "http://www.thegodfather.com/",
    releaseDate = "1972-03-14",
    backdropImage = "https://image.tmdb.org/t/p/w1280/tmU7GeKVybMWFButWEGl2M4GeiP.jpg",
    posterImage = "https://image.tmdb.org/t/p/w500/tmU7GeKVybMWFButWEGl2M4GeiP.jpg",
    providerName = DEFAULT_FILM_SOURCE_NAME,
    overview = "Spanning the years 1945 to 1955, a chronicle of the fictional Italian-American Corleone crime family. When organized crime family patriarch, Vito Corleone barely survives an attempt on his life, his youngest son, Michael steps in to take care of the would-be killers, launching a campaign of bloody revenge.",
)