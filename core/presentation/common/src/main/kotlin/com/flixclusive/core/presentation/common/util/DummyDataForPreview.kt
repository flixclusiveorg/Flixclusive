package com.flixclusive.core.presentation.common.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.Movie
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
    @Composable
    fun getDummyProviderMetadata() =
        remember {
            ProviderMetadata(
                authors = List(5) { Author("FLX $it") },
                repositoryUrl = "https://github.com/flixclusiveorg/123Movies",
                buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/plugins-template/builds/updater.json",
                changelog =
                    """
                    # v1.0.0

                    - Added new feature
                    - Fixed a bug
                    """.trimIndent(),
                versionName = "1.0.0",
                versionCode = 10000,
                description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                iconUrl = null,
                language = Language.Multiple,
                name = "CineFlix",
                providerType = ProviderType.All,
                status = Status.Working,
                id = "TEST-FLX-PROVIDER",
            )
        }

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
                name = it
            )
        }
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
        genres = genres.map {
            Genre(
                id = it.hashCode(),
                name = it
            )
        }
    )
}
