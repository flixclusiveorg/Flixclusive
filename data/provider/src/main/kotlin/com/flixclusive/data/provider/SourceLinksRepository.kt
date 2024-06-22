package com.flixclusive.data.provider

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.FilmDetails
import com.flixclusive.model.tmdb.common.tv.Episode
import com.flixclusive.provider.ProviderApi

interface SourceLinksRepository {

    suspend fun getSourceLinks(
        providerApi: ProviderApi,
        watchId: String,
        film: FilmDetails,
        episodeData: Episode?,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ): Resource<Unit?>

    suspend fun getWatchId(
        film: FilmDetails?,
        providerApi: ProviderApi,
    ): Resource<String?>
}