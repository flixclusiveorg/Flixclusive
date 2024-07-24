package com.flixclusive.model.tmdb

import com.flixclusive.model.tmdb.common.details.Company
import kotlinx.serialization.Serializable

/**
 * An abstract representation of detailed film information.
 *
 * @property producers The production companies involved in the film.
 * @property tagLine The tagline of the film.
 * @property cast The cast of the film.
 *
 * @see Film
 * @see Movie
 * @see TvShow
 */
@Serializable
abstract class FilmDetails : Film() {
    abstract val producers: List<Company>
    abstract val tagLine: String?
    abstract val cast: List<Person>

    companion object {
        val FilmDetails.isMovie
            get() = this is Movie

        val FilmDetails.isTvShow
            get() = this is TvShow
    }
}

/*
Same properties with data types:
  - spoken_languages
  - production_countries
  - genres
  - original_language
  - backdrop_path
  - production_companies
  - poster_path
  - overview
  - adult
  - vote_count
  - id
  - origin_country
  - status
  - vote_average
  - homepage
  - tagline
  - popularity
* */