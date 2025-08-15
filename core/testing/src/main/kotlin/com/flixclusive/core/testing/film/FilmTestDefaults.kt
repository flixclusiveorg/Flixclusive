package com.flixclusive.core.testing.film

import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.film.util.FilmType

/**
 * Provides default values for testing purposes.
 *
 * This is useful for creating mock data in tests without needing to define
 */
object FilmTestDefaults {
    const val DEFAULT_DESCRIPTION = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."

    /**
     * Returns a default [Movie] instance for testing purposes.
     *
     * Default values are set to represent a well-known movie, "The Godfather".
     * */
    fun getMovie(
        id: String = "238",
        tmdbId: Int = 238,
        imdbId: String = "tt0068646",
        title: String = "The Godfather",
        rating: Double = 8.691,
        year: Int = 1972,
        homePage: String = "http://www.thegodfather.com/",
        releaseDate: String = "1972-03-14",
        backdropImage: String = "https://image.tmdb.org/t/p/w1280/tmU7GeKVybMWFButWEGl2M4GeiP.jpg",
        posterImage: String = "https://image.tmdb.org/t/p/w500/tmU7GeKVybMWFButWEGl2M4GeiP.jpg",
        providerId: String = DEFAULT_FILM_SOURCE_NAME,
        overview: String = DEFAULT_DESCRIPTION,
        recommendations: List<FilmSearchItem> = emptyList(),
        genres: List<Genre> = listOf(
            Genre(id = 18, name = "Drama"),
            Genre(id = 80, name = "Crime")
        ),
    ) = Movie(
        id = id,
        tmdbId = tmdbId,
        imdbId = imdbId,
        title = title,
        rating = rating,
        year = year,
        homePage = homePage,
        releaseDate = releaseDate,
        backdropImage = backdropImage,
        posterImage = posterImage,
        providerId = providerId,
        overview = overview,
        genres = genres,
        recommendations = recommendations
    )

    /**
     * Returns a default [TvShow] instance for testing purposes.
     * */
    fun getTvShow(
        id: String = "1399",
        tmdbId: Int = 1399,
        imdbId: String = "tt0903747",
        title: String = "Game of Thrones",
        rating: Double = 8.5,
        year: Int = 2011,
        homePage: String = "https://www.hbo.com/game-of-thrones",
        releaseDate: String = "2011-04-17",
        backdropImage: String = "https://image.tmdb.org/t/p/w1280/8hP9D4d2b6c3a2e3f4f5e6f7g8h9i0j.jpg",
        posterImage: String = "https://image.tmdb.org/t/p/w500/8hP9D4d2b6c3a2e3f4f5e6f7g8h9i0j.jpg",
        providerId: String = DEFAULT_FILM_SOURCE_NAME,
        overview: String = DEFAULT_DESCRIPTION,
        genres: List<Genre> = listOf(
            Genre(id = 18, name = "Drama"),
            Genre(id = 80, name = "Crime")
        ),
        seasons: List<Season> = List(10) { index ->
            getSeason(
                overview = DEFAULT_DESCRIPTION,
                name = "Season ${index + 1}",
                airDate = "2011-04-${17 + index}",
                number = index + 1,
                episodes = List(10) { episodeIndex ->
                    getEpisode(
                        id = "episode-${index + 1}-${episodeIndex + 1}",
                        number = episodeIndex + 1,
                        title = "Episode ${episodeIndex + 1}",
                        airDate = "2011-04-${17 + index}-${episodeIndex + 1}",
                    )
                },
            )
        },
    ) = TvShow(
        id = id,
        tmdbId = tmdbId,
        imdbId = imdbId,
        title = title,
        rating = rating,
        year = year,
        homePage = homePage,
        releaseDate = releaseDate,
        backdropImage = backdropImage,
        posterImage = posterImage,
        providerId = providerId,
        overview = overview,
        seasons = seasons,
        totalSeasons = seasons.size,
        totalEpisodes = seasons.flatMap { it.episodes }.size,
        genres = genres,
    )

    /**
     * Returns a default [Episode] instance for testing purposes.
     * */
    fun getEpisode(
        id: String = "12345",
        overview: String = DEFAULT_DESCRIPTION,
        runtime: Int? = 60,
        number: Int = 1,
        title: String = "Winter Is Coming",
        airDate: String = "2011-04-17",
        season: Int = 1,
        image: String = "https://image.tmdb.org/t/p/w500/8hP9D4d2b6c3a2e3f4f5e6f7g8h9i0j.jpg",
        rating: Double = 8.5,
    ) = Episode(
        id = id,
        overview = overview,
        runtime = runtime,
        number = number,
        title = title,
        airDate = airDate,
        season = season,
        image = image,
        rating = rating,
    )

    /**
     * Returns a default [Season] instance for testing purposes.
     * */
    fun getSeason(
        overview: String = DEFAULT_DESCRIPTION,
        name: String = "Season 1",
        airDate: String = "2011-04-17",
        episodes: List<Episode> = List(10) { index ->
            getEpisode(
                id = "episode-${index + 1}",
                number = index + 1,
                title = "Episode ${index + 1}",
                airDate = "2011-04-${17 + index}",
            )
        },
        episodeCount: Int = episodes.size,
        rating: Double = 8.5,
        number: Int = 1,
        image: String = "https://image.tmdb.org/t/p/w500/8hP9D4d2b6c3a2e3f4f5e6f7g8h9i0j.jpg",
    ) = Season(
        overview = overview,
        name = name,
        airDate = airDate,
        episodeCount = episodeCount,
        rating = rating,
        number = number,
        image = image,
        episodes = episodes,
    )

    fun getFilmSearchItem(
        id: String? = null,
        providerId: String = DEFAULT_FILM_SOURCE_NAME,
        filmType: FilmType = FilmType.MOVIE,
        homePage: String? = "https://example.com",
        title: String = "Example Film",
        posterImage: String? = "https://image.tmdb.org/t/p/w500/example.jpg",
        adult: Boolean = false,
        backdropImage: String? = "https://image.tmdb.org/t/p/w1280/example_backdrop.jpg",
        imdbId: String? = "tt1234567",
        tmdbId: Int? = 12345,
        releaseDate: String? = "2023-01-01",
        rating: Double? = 7.5,
        year: Int = 2023,
        logoImage: String? = null,
        genres: List<Genre> = emptyList(),
        customProperties: Map<String, String> = emptyMap(),
        voteCount: Int = 100,
    ) = FilmSearchItem(
        id = id,
        providerId = providerId,
        filmType = filmType,
        homePage = homePage,
        title = title,
        posterImage = posterImage,
        adult = adult,
        backdropImage = backdropImage,
        imdbId = imdbId,
        tmdbId = tmdbId,
        releaseDate = releaseDate,
        rating = rating,
        year = year,
        logoImage = logoImage,
        genres = genres,
        customProperties = customProperties,
        voteCount = voteCount
    )
}
