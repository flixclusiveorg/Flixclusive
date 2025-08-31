package com.flixclusive.core.presentation.common.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.util.FilmType
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

    @Composable
    fun getDummyFilm() =
        remember {
            FilmSearchItem(
                id = null,
                title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
                posterImage = "https://image.tmdb.org/t/p/w500/t9XkeE7HzOsdQcDDDapDYh8Rrmt.jpg",
                providerId = DEFAULT_FILM_SOURCE_NAME,
                filmType = FilmType.MOVIE,
                homePage = null,
            )
        }
}
