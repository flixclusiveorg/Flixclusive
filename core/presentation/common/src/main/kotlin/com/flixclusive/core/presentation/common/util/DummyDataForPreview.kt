package com.flixclusive.core.presentation.common.util

import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.Show
import com.flixclusive.model.media.common.Company
import com.flixclusive.model.media.common.Genre
import com.flixclusive.model.media.common.MediaIdSource
import com.flixclusive.model.media.common.MediaType
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.model.provider.Author
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderStatus
import com.flixclusive.model.provider.ProviderType

/**
 * Dummy data for compose previews
 * */
object DummyDataForPreview {
    fun getProviderMetadata(
        id: String = "TEST-FLX-PROVIDER",
        name: String = "Test provider",
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
        status: ProviderStatus = ProviderStatus.Working,
        language: Language = Language.Multiple,
        authors: List<Author> = List(5) {
            Author(
                "FLX $it",
                socialLink = if (it % 2 == 0) "https://github.com/john-doe" else null
            )
        },
        adult: Boolean = false,
    ) =
        ProviderMetadata(
            adult = adult,
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

    fun getMedia(
        id: String = "123",
        title: String = "Sample item",
        providerId: String = "id-tmdb-123",
        mediaType: MediaType = MediaType.MOVIE,
        genres: List<String> = listOf("Action", "Adventure"),
        posterImage: String? = "/t9XkeE7HzOsdQcDDDapDYh8Rrmt.jpg",
        backdropImage: String? = "/4kTINu9mv2YV1PqFqPGG1FZMnhi.jpg",
        logoImage: String? = "/6pObznbCoxVpY1lPQwJxETd7Phe.png",
        rating: Double? = 7.5,
        releaseDate: Long? = System.currentTimeMillis(),
        overview: String? = "This is a sample overview for the media.",
        homePage: String? = null,
        externalIds: Map<MediaIdSource, String> = mapOf(
            MediaIdSource.IMDB to "tt1234567",
            MediaIdSource.TMDB to "123",
        ),
    ) = PartialMedia(
        id = id,
        externalIds = externalIds,
        title = title,
        posterImage = posterImage,
        backdropImage = backdropImage,
        logoImage = logoImage,
        providerId = providerId,
        type = mediaType,
        releaseDate = releaseDate,
        rating = rating,
        overview = overview,
        homePage = homePage,
        genres = genres.map { Genre(name = it) },
    )

    fun getMovie(
        id: String = "123",
        title: String = "Sample item",
        providerId: String = "id-tmdb-123",
        genres: List<String> = listOf("Action", "Adventure"),
        posterImage: String? = "/t9XkeE7HzOsdQcDDDapDYh8Rrmt.jpg",
        backdropImage: String? = "/4kTINu9mv2YV1PqFqPGG1FZMnhi.jpg",
        logoImage: String? = "/6pObznbCoxVpY1lPQwJxETd7Phe.png",
        rating: Double? = 7.5,
        runtime: Int? = 100,
        language: String? = "en",
        releaseDate: Long? = System.currentTimeMillis(),
        overview: String? = "This is a sample overview for the media.",
        productionCompanies: List<String> = listOf("Marvel Studios", "Pixar"),
        homePage: String? = null,
        externalIds: Map<MediaIdSource, String> = mapOf(
            MediaIdSource.IMDB to "tt1234567",
            MediaIdSource.TMDB to "123",
        ),
    ) = Movie(
        id = id,
        title = title,
        posterImage = posterImage,
        backdropImage = backdropImage,
        logoImage = logoImage,
        providerId = providerId,
        releaseDate = releaseDate,
        rating = rating,
        overview = overview,
        homePage = homePage,
        runtime = runtime,
        language = language,
        recommendations = List(20) { getMedia(id = "$it") },
        externalIds = externalIds,
        producers = productionCompanies.map { Company(name = it) },
        genres = genres.map { Genre(name = it) },
    )

    fun getShow(
        id: String = "123",
        title: String = "Sample item",
        providerId: String = "id-tmdb-123",
        genres: List<String> = listOf("Action", "Adventure"),
        posterImage: String? = "/t9XkeE7HzOsdQcDDDapDYh8Rrmt.jpg",
        backdropImage: String? = "/4kTINu9mv2YV1PqFqPGG1FZMnhi.jpg",
        logoImage: String? = "/6pObznbCoxVpY1lPQwJxETd7Phe.png",
        rating: Double? = 7.5,
        runtime: Int? = 100,
        language: String? = "en",
        releaseDate: Long? = System.currentTimeMillis(),
        overview: String? = "This is a sample overview for the media.",
        homePage: String? = null,
        networks: List<String> = listOf("Netflix", "HBO"),
        externalIds: Map<MediaIdSource, String> = mapOf(
            MediaIdSource.IMDB to "tt1234567",
            MediaIdSource.TMDB to "123",
        ),
        seasons: List<Season> = List(3) {
            val season = it + 1
            val episodes = List(10) { ep ->
                val airDate = System.currentTimeMillis() + (-season * 10 + ep) * 24 * 60 * 60 * 1000L
                Episode(
                    id = "$it-$ep",
                    title = "Episode ${ep + 1}",
                    number = ep + 1,
                    season = season,
                    overview = "This is a sample overview for episode ${ep + 1}.",
                    releaseDate = airDate, // Future release dates
                    runtime = 20 + ep,
                    image = "/9hGF3WUkBf7cSjMg0cdMDHJkByd.jpg",
                    isReleased = airDate <= System.currentTimeMillis(),
                    rating = 5.0 + (ep * 0.1),
                )
            }

            val airDate = System.currentTimeMillis() + -season * 30L * 24 * 60 * 60 * 1000

            Season.Full(
                overview = "This is a sample overview for season ${it + 1}.",
                releaseDate = airDate,
                isReleased = airDate <= System.currentTimeMillis(),
                image = "/wgfKiqzuMrFIkU1M68DDDY8kGC1.jpg",
                episodes = episodes,
                rating = 6.0 + (it * 0.5),
                number = season,
                id = "$season",
            )
        },
    ) = Show(
        id = id,
        title = title,
        posterImage = posterImage,
        backdropImage = backdropImage,
        logoImage = logoImage,
        providerId = providerId,
        releaseDate = releaseDate,
        rating = rating,
        overview = overview,
        homePage = homePage,
        runtime = runtime,
        language = language,
        recommendations = List(20) { getMedia(id = "$it") },
        seasons = seasons,
        totalEpisodes = seasons.sumOf { it.episodeCount },
        totalSeasons = seasons.size,
        externalIds = externalIds,
        networks = networks.map { Company(name = it) },
        genres = genres.map { Genre(name = it) },
    )
}
