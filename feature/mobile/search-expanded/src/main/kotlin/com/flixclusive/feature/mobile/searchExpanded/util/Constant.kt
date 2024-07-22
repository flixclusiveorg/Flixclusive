package com.flixclusive.feature.mobile.searchExpanded.util

import com.flixclusive.gradle.entities.Language
import com.flixclusive.gradle.entities.ProviderData
import com.flixclusive.gradle.entities.ProviderType
import com.flixclusive.gradle.entities.Status
import com.flixclusive.model.tmdb.DEFAULT_FILM_SOURCE_NAME

object Constant {
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