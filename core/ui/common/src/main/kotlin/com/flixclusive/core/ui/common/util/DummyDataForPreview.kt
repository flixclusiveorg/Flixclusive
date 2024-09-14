package com.flixclusive.core.ui.common.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.provider.Author
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.provider.Provider
import com.flixclusive.provider.ProviderApi
import okhttp3.OkHttpClient

object DummyDataForPreview {
    @Composable
    fun getDummyProviderData() = remember {
        ProviderData(
            authors = List(5) { Author("FLX $it") },
            repositoryUrl = "https://github.com/flixclusiveorg/123Movies",
            buildUrl = "https://raw.githubusercontent.com/flixclusiveorg/plugins-template/builds/updater.json",
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
    fun getDummyProviderApi(): List<ProviderApi> {
        return remember {
            val provider = object : Provider() {
                override fun getApi(context: Context, client: OkHttpClient)
                    = throw NotImplementedError()
            }

            return@remember List<ProviderApi>(5) {
                object : ProviderApi(
                    OkHttpClient(),
                    provider
                ) {

                }
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