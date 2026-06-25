package com.flixclusive.core.navigation.navigator

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

interface NavigateToManageMediaLinksScreen {
    fun navigateToManageMediaLinksScreen(
        media: MediaMetadata,
        episode: Episode? = null
    )
}
