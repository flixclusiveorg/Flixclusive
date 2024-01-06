package com.flixclusive.domain.repository

import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.providers.sources.SourceProvider
import com.flixclusive.providers.models.common.SourceLink
import com.flixclusive.providers.models.common.Subtitle

interface SourceLinksRepository {

    suspend fun getSourceLinks(
        mediaId: String,
        provider: SourceProvider,
        season: Int? = null,
        episode: Int? = null,
        onLinkLoaded: (SourceLink) -> Unit,
        onSubtitleLoaded: (Subtitle) -> Unit,
    ): Resource<Unit?>

    suspend fun getMediaId(
        film: Film?,
        provider: SourceProvider,
    ): String?
}