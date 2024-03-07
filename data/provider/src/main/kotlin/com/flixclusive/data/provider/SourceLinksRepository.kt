package com.flixclusive.data.provider

import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.provider.Provider

interface SourceLinksRepository {

    suspend fun getSourceLinks(
        mediaId: String,
        provider: Provider,
        season: Int? = null,
        episode: Int? = null,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ): Resource<Unit?>

    suspend fun getMediaId(
        film: Film?,
        provider: Provider,
    ): String?
}