package com.flixclusive.core.presentation.common.util

import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Movie
import com.flixclusive.model.film.TvShow
import com.flixclusive.model.film.common.details.Company
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.film.util.extractYear
import com.flixclusive.model.provider.Author
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status

/**
 * Dummy data for compose previews
 * */
object DummyDataForPreview {
    fun getDummyProviderMetadata(
        id: String = "TEST-FLX-PROVIDER",
        name: String = DEFAULT_FILM_SOURCE_NAME,
        description: String = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        repositoryUrl: String = "https://github.com/flixclusiveorg/123Movies",
        buildUrl: String = "https://raw.githubusercontent.com/flixclusiveorg/plugins-template/builds/updater.json",
        changelog: String =
            """
            # v1.0.0

            - Added new feature
            - Fixed a bug
            """.trimIndent(),
        versionName: String = "1.0.0",
        versionCode: Long = 10000,
        iconUrl: String? = "https://i.imgur.com/qd6zqII.png", // TMDB Icon
        providerType: ProviderType = ProviderType.All,
        status: Status = Status.Working,
        language: Language = Language.Multiple,
        authors: List<Author> = List(5) { Author("FLX $it") },
    ) =
        ProviderMetadata(
            id = id,
            name = name,
            description = description,
            repositoryUrl = repositoryUrl,
            buildUrl = buildUrl,
            changelog = changelog,
            versionName = versionName,
            versionCode = versionCode,
            iconUrl = iconUrl,
            providerType = providerType,
            status = status,
            language = language,
            authors = authors,
        )

    fun getFilm(
        id: String? = null,
        tmdbId: Int = 123,
        imdbId: String = "tt1234567",
        title: String = "Sample item",
        providerId: String = DEFAULT_FILM_SOURCE_NAME,
        filmType: FilmType = FilmType.MOVIE,
        genres: List<String> = listOf("Action", "Adventure"),
        posterImage: String? = "/t9XkeE7HzOsdQcDDDapDYh8Rrmt.jpg",
        backdropImage: String? = "/4kTINu9mv2YV1PqFqPGG1FZMnhi.jpg",
        logoImage: String? = "/6pObznbCoxVpY1lPQwJxETd7Phe.png",
        rating: Double? = 7.5,
        releaseDate: String? = "2023-10-10",
        overview: String? = "This is a sample overview for the film.",
        homePage: String? = null,
    ) = FilmSearchItem(
        id = id,
        tmdbId = tmdbId,
        imdbId = imdbId,
        title = title,
        posterImage = posterImage,
        backdropImage = backdropImage,
        logoImage = logoImage,
        providerId = providerId,
        filmType = filmType,
        releaseDate = releaseDate,
        rating = rating,
        overview = overview,
        homePage = homePage,
        genres = genres.map {
            Genre(
                id = it.hashCode(),
                name = it,
            )
        },
    )

    fun getMovie(
        id: String? = null,
        tmdbId: Int = 123,
        imdbId: String = "tt1234567",
        title: String = "Sample item",
        providerId: String = DEFAULT_FILM_SOURCE_NAME,
        genres: List<String> = listOf("Action", "Adventure"),
        posterImage: String? = "/t9XkeE7HzOsdQcDDDapDYh8Rrmt.jpg",
        backdropImage: String? = "/4kTINu9mv2YV1PqFqPGG1FZMnhi.jpg",
        logoImage: String? = "/6pObznbCoxVpY1lPQwJxETd7Phe.png",
        rating: Double? = 7.5,
        runtime: Int? = 100,
        language: String? = "en",
        releaseDate: String? = "2023-10-10",
        overview: String? = "This is a sample overview for the film.",
        productionCompanies: List<String> = listOf("Marvel Studios", "Pixar"),
        homePage: String? = null,
    ) = Movie(
        id = id,
        tmdbId = tmdbId,
        imdbId = imdbId,
        title = title,
        posterImage = posterImage,
        backdropImage = backdropImage,
        logoImage = logoImage,
        providerId = providerId,
        releaseDate = releaseDate,
        year = releaseDate?.extractYear(),
        rating = rating,
        overview = overview,
        homePage = homePage,
        runtime = runtime,
        language = language,
        recommendations = List(20) { getFilm(id = "$it") },
        producers = productionCompanies.map {
            Company(
                id = it.hashCode(),
                name = it,
                logoPath = null,
            )
        },
        genres = genres.map {
            Genre(
                id = it.hashCode(),
                name = it,
            )
        },
    )

    fun getTvShow(
        id: String? = null,
        tmdbId: Int = 123,
        imdbId: String = "tt1234567",
        title: String = "Sample item",
        providerId: String = DEFAULT_FILM_SOURCE_NAME,
        genres: List<String> = listOf("Action", "Adventure"),
        posterImage: String? = "/t9XkeE7HzOsdQcDDDapDYh8Rrmt.jpg",
        backdropImage: String? = "/4kTINu9mv2YV1PqFqPGG1FZMnhi.jpg",
        logoImage: String? = "/6pObznbCoxVpY1lPQwJxETd7Phe.png",
        rating: Double? = 7.5,
        runtime: Int? = 100,
        language: String? = "en",
        releaseDate: String? = "2023-10-10",
        overview: String? = "This is a sample overview for the film.",
        homePage: String? = null,
        productionCompanies: List<String> = listOf("Marvel Studios", "Pixar"),
        networks: List<String> = listOf("Netflix", "HBO"),
        seasons: List<Season> = List(3) {
            val season = it + 1
            val episodes = List(10) { ep ->
                Episode(
                    id = "$it-$ep",
                    title = "Episode ${ep + 1}",
                    number = ep + 1,
                    season = season,
                    overview = "This is a sample overview for episode ${ep + 1}.",
                    airDate = "202$season-10-${ep + 1}",
                    runtime = 20 + ep,
                    image = "/9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg",
                    rating = 5.0 + (ep * 0.1),
                )
            }

            Season(
                overview = "This is a sample overview for season ${it + 1}.",
                airDate = "202$season-10-1",
                name = "Season $season",
                image = "/wgfKiqzuMrFIkU1M68DDDY8kGC1.jpg",
                episodes = episodes,
                episodeCount = episodes.size,
                rating = 6.0 + (it * 0.5),
                number = season,
            )
        },
    ) = TvShow(
        id = id,
        tmdbId = tmdbId,
        imdbId = imdbId,
        title = title,
        posterImage = posterImage,
        backdropImage = backdropImage,
        logoImage = logoImage,
        providerId = providerId,
        releaseDate = releaseDate,
        year = releaseDate?.extractYear(),
        rating = rating,
        overview = overview,
        homePage = homePage,
        runtime = runtime,
        language = language,
        recommendations = List(20) { getFilm(id = "$it") },
        seasons = seasons,
        totalEpisodes = seasons.sumOf { it.episodes.size },
        totalSeasons = seasons.size,
        producers = productionCompanies.map {
            Company(
                id = it.hashCode(),
                name = it,
                logoPath = null,
            )
        },
        networks = networks.map {
            Company(
                id = it.hashCode(),
                name = it,
                logoPath = null,
            )
        },
        genres = genres.map {
            Genre(
                id = it.hashCode(),
                name = it,
            )
        },
    )
}
