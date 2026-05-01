package com.flixclusive.core.testing.media

import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.Genre
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season

/**
 * Provides default values for testing purposes.
 *
 * This is useful for creating mock data in tests without needing to define
 */
object MediaTestDefaults {
    const val DEFAULT_DESCRIPTION = "Lorem ipsum dolor sit amet, consectetur adipiscing elit."
    const val DEFAULT_PROVIDER_ID = "the-movie-db123"

    /**
     * Returns a default [Movie] instance for testing purposes.
     *
     * Default values are set to represent a well-known movie, "The Godfather".
     * */
    fun getMovie(
        id: String = "238",
        title: String = "The Godfather",
        rating: Double = 8.691,
        homePage: String = "http://www.thegodfather.com/",
        releaseDate: Long = 735964800000, // 1972-03-14 in milliseconds
        backdropImage: String = "https://image.tmdb.org/t/p/w1280/tmU7GeKVybMWFButWEGl2M4GeiP.jpg",
        posterImage: String = "https://image.tmdb.org/t/p/w500/tmU7GeKVybMWFButWEGl2M4GeiP.jpg",
        providerId: String = DEFAULT_PROVIDER_ID,
        overview: String = DEFAULT_DESCRIPTION,
        recommendations: List<PartialMedia> = emptyList(),
        genres: List<Genre> = listOf(
            Genre(name = "Drama"),
            Genre(name = "Crime")
        ),
    ) = Movie(
        id = id,
        title = title,
        rating = rating,
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
     * Returns a default [Show] instance for testing purposes.
     * */
    fun getShow(
        id: String = "1399",
        title: String = "Game of Thrones",
        rating: Double = 8.5,
        homePage: String = "https://www.hbo.com/game-of-thrones",
        releaseDate: Long = 1303056000000, // 2011-04-17 in milliseconds
        backdropImage: String = "https://image.tmdb.org/t/p/w1280/8hP9D4d2b6c3a2e3f4f5e6f7g8h9i0j.jpg",
        posterImage: String = "https://image.tmdb.org/t/p/w500/8hP9D4d2b6c3a2e3f4f5e6f7g8h9i0j.jpg",
        providerId: String = DEFAULT_PROVIDER_ID,
        overview: String = DEFAULT_DESCRIPTION,
        genres: List<Genre> = listOf(
            Genre(name = "Drama"),
            Genre(name = "Crime")
        ),
        seasons: List<Season> = List(10) { index ->
            getSeason(
                overview = DEFAULT_DESCRIPTION,
                title = "Season ${index + 1}",
                releaseDate = 1303056000000 + index * 365L * 24 * 60 * 60 * 1000, // Each season airs a year apart
                number = index + 1,
                episodes = List(10) { episodeIndex ->
                    getEpisode(
                        id = "episode-${index + 1}-${episodeIndex + 1}",
                        number = episodeIndex + 1,
                        title = "Episode ${episodeIndex + 1}",
                        releaseDate = 1303056000000 + index * 365L * 24 * 60 * 60 * 1000 + episodeIndex * 7L * 24 * 60 * 60 * 1000, // Each episode airs a week apart
                    )
                },
            )
        },
    ) = Show(
        id = id,
        title = title,
        rating = rating,
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
        releaseDate: Long = 1303056000000, // 2011-04-17 in milliseconds
        season: Int = 1,
        image: String = "https://image.tmdb.org/t/p/w500/8hP9D4d2b6c3a2e3f4f5e6f7g8h9i0j.jpg",
        rating: Double = 8.5,
        isReleased: Boolean = true,
    ) = Episode(
        id = id,
        overview = overview,
        runtime = runtime,
        number = number,
        title = title,
        releaseDate = releaseDate,
        season = season,
        image = image,
        rating = rating,
        isReleased = isReleased,
    )

    /**
     * Returns a default [Season] instance for testing purposes.
     * */
    fun getSeason(
        id: String = "123",
        overview: String = DEFAULT_DESCRIPTION,
        title: String = "Season 1",
        releaseDate: Long = 1303056000000, // 2011-04-17 in milliseconds
        episodes: List<Episode> = List(10) { index ->
            getEpisode(
                id = "episode-${index + 1}",
                number = index + 1,
                title = "Episode ${index + 1}",
                releaseDate = 1303056000000 + index * 7 * 24 * 60 * 60 * 1000L, // Each episode airs a week apart
            )
        },
        episodeCount: Int = episodes.size,
        rating: Double = 8.5,
        number: Int = 1,
        image: String = "https://image.tmdb.org/t/p/w500/8hP9D4d2b6c3a2e3f4f5e6f7g8h9i0j.jpg",
        isReleased: Boolean = true,
    ) = Season(
        overview = overview,
        title = title,
        releaseDate = releaseDate,
        episodeCount = episodeCount,
        rating = rating,
        number = number,
        image = image,
        episodes = episodes,
        isReleased = isReleased,
        id = id
    )

    fun getPartialMedia(
        id: String = "123",
        providerId: String = DEFAULT_PROVIDER_ID,
        mediaType: MediaType = MediaType.MOVIE,
        homePage: String? = "https://example.com",
        title: String = "Example MediaMetadata",
        posterImage: String? = "https://image.tmdb.org/t/p/w500/example.jpg",
        adult: Boolean = false,
        backdropImage: String? = "https://image.tmdb.org/t/p/w1280/example_backdrop.jpg",
        releaseDate: Long? = 1609459200000, // 2021-01-01 in milliseconds
        rating: Double? = 7.5,
        logoImage: String? = null,
        genres: List<Genre> = emptyList(),
        customProperties: Map<String, String> = emptyMap(),
    ) = PartialMedia(
        id = id,
        providerId = providerId,
        type = mediaType,
        homePage = homePage,
        title = title,
        posterImage = posterImage,
        adult = adult,
        backdropImage = backdropImage,
        releaseDate = releaseDate,
        rating = rating,
        logoImage = logoImage,
        genres = genres,
        customProperties = customProperties,
    )
}
