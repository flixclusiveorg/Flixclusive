package com.flixclusive.provider.util

import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.TMDBEpisode

interface WebViewCallback {
    suspend fun onSuccess(episode: TMDBEpisode?)

    suspend fun onError()


    fun onSubtitleLoaded(subtitle: Subtitle)

    fun onLinkLoaded(link: SourceLink)

    suspend fun updateDialogState(state: SourceDataState)
}