package com.flixclusive.feature.mobile.searchExpanded.util

import androidx.compose.runtime.Stable
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME
import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status

internal object Constant {
    const val TMDB_PROVIDER_ID = "tmdb-default-wtf"

    @Stable
    val tmdbProviderMetadata by lazy {
        ProviderMetadata(
            authors = emptyList(),
            repositoryUrl = "",
            buildUrl = "",
            description = null,
            versionName = "1.0.0",
            versionCode = 10000,
            iconUrl = "https://i.imgur.com/qd6zqII.png",
            language = Language.Multiple,
            name = "$DEFAULT_FILM_SOURCE_NAME - Default",
            providerType = ProviderType.All,
            status = Status.Working,
            id = TMDB_PROVIDER_ID,
        )
    }
}
