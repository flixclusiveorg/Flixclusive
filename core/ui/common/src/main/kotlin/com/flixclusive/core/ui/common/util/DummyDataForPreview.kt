package com.flixclusive.core.ui.common.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.gradle.entities.Author
import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status
import com.flixclusive.model.provider.MediaLink
import com.flixclusive.model.tmdb.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.FilmSearchItem
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.ProviderApi
import okhttp3.OkHttpClient

object DummyDataForPreview {
    @Composable
    fun getDummyProviderData() = remember {
        ProviderData(
            authors = List(5) { Author("FLX $it") },
            repositoryUrl = "https://github.com/flixclusive/123Movies",
            buildUrl = "https://raw.githubusercontent.com/Flixclusive/plugins-template/builds/updater.json",
            changelog = """
                # v1.0.0
                
                - Added new feature
                - Fixed a bug
            """.trimIndent(),
            versionName = "1.0.0",
            versionCode = 10000,
            description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            iconUrl = null,
            language = Language.Multiple,
            name = "CineFlix",
            providerType = ProviderType.All,
            status = Status.Working
        )
    }

    @Composable
    fun getDummyProviderApi() = remember {
        List<ProviderApi>(5) {
            object : ProviderApi(OkHttpClient()) {
                override val name: String = "FLX $it"

                override suspend fun getLinks(
                    watchId: String,
                    film: FilmDetails,
                    episode: Episode?
                ): List<MediaLink> = emptyList()
            }
        }
    }

    @Composable
    fun getDummyFilm() = remember {
        FilmSearchItem(
            id = null,
            title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
            posterImage = "https://image.tmdb.org/t/p/w500/t9XkeE7HzOsdQcDDDapDYh8Rrmt.jpg",
            providerName = DEFAULT_FILM_SOURCE_NAME,
            filmType = FilmType.MOVIE,
            homePage = null
        )
    }
}