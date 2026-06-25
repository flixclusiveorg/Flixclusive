package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

interface NavigateToMediaLinksBottomSheet : NavigateBack {
    fun showPlayerSplashScreen(
        media: MediaMetadata,
        episode: Episode?,
        initialStreamUrl: String? = null,
        initialCacheId: String? = null,
        initialHeaders: Map<String, String>? = null
    )
}
