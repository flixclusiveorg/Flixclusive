package com.flixclusive.feature.mobile.searchExpanded.util

import com.flixclusive.model.provider.Language
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.model.provider.ProviderType
import com.flixclusive.model.provider.Status
import com.flixclusive.model.film.DEFAULT_FILM_SOURCE_NAME

internal object Constant {
    val tmdbProviderData = ProviderData(
        authors = emptyList(),
        repositoryUrl = null,
        buildUrl = null,
        description = null,
        versionName = "1.0.0",
        versionCode = 10000,
        iconUrl = "https://i.imgur.com/qd6zqII.png",
        language = Language.Multiple,
        name = "$DEFAULT_FILM_SOURCE_NAME - Default",
        providerType = ProviderType.All,
        status = Status.Working
    )
}