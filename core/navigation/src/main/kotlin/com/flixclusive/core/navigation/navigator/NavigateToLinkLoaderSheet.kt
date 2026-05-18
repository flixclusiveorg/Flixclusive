package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

interface NavigateToLinkLoaderSheet {
    fun showLinkLoaderSheet(media: MediaMetadata, episode: Episode? = null)
}
