package com.flixclusive.provider.util

import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.common.tv.Episode

interface WebViewCallback {
    suspend fun onSuccess(episode: Episode?)


    fun onSubtitleLoaded(subtitle: Subtitle)

    fun onLinkLoaded(link: SourceLink)

    suspend fun updateDialogState(state: SourceDataState)
}