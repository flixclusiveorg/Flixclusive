package com.flixclusive.domain.usecase

import com.flixclusive.domain.model.SourceData
import com.flixclusive.domain.model.SourceDataState
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.providers.sources.SourceProvider
import kotlinx.coroutines.flow.Flow

interface SourceLinksProviderUseCase {
    val providers: List<SourceProvider>

    fun loadLinks(
        film: Film,
        watchHistoryItem: WatchHistoryItem?,
        preferredProviderName: String? = null,
        isChangingProvider: Boolean = false,
        mediaId: String? = null,
        episode: TMDBEpisode? = null,
        onSuccess: (TMDBEpisode?) -> Unit,
        onError: (() -> Unit)? = null,
    ): Flow<SourceDataState>

    fun getLinks(
        filmId: Int,
        episode: TMDBEpisode? = null,
    ): SourceData

    fun clearCache()
}