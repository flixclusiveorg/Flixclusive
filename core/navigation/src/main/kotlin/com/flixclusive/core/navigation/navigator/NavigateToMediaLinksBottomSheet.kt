package com.flixclusive.core.navigation.navigator

import com.flixclusive.core.navigation.navargs.PlaybackRequest

interface NavigateToMediaLinksBottomSheet : NavigateBack {
    fun showPlayerSplashScreen(request: PlaybackRequest)
}
