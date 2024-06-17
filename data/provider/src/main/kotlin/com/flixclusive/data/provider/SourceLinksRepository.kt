package com.flixclusive.data.provider

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.provider.ProviderApi

interface SourceLinksRepository {

    suspend fun getSourceLinks(
        providerApi: ProviderApi,
        watchId: String,
        film: FilmDetails,
        season: Int? = null,
        episode: Int? = null,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ): Resource<Unit?>

    suspend fun getWatchId(
        film: FilmDetails?,
        providerApi: ProviderApi,
    ): Resource<String?>
}