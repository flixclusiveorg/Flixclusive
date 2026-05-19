package com.flixclusive.feature.mobile.media.navigator

import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

interface NavigatorMediaLinksBottomSheet : NavigateBack {
    fun showPlayerSplashScreen(
        media: MediaMetadata,
        episode: Episode?
    )

    // TODO: Support this soon
//    fun playMediaWithLink(
//        media: MediaMetadata,
//        episode: Episode?,
//        link: Stream
//    )
}
