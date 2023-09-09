package com.flixclusive.presentation.mobile.screens.preferences.content

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.flixclusive.R
import com.flixclusive.presentation.destinations.AboutMobileScreenDestination
import com.flixclusive.presentation.destinations.RecentlyWatchedMobileScreenDestination
import com.flixclusive.presentation.destinations.SettingsMobileScreenDestination
import com.flixclusive.presentation.destinations.WatchlistMobileScreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec

enum class PreferencesItems(
    @DrawableRes val iconId: Int,
    @StringRes val labelId: Int,
    val direction: DirectionDestinationSpec? = null
) {
    Watchlist(
        iconId = R.drawable.round_library,
        labelId = R.string.watchlist,
        direction = WatchlistMobileScreenDestination
    ),
    RecentlyWatched(
        iconId = R.drawable.time_circle,
        labelId = R.string.recently_watched,
        direction = RecentlyWatchedMobileScreenDestination
    ),
    Settings(
        iconId = R.drawable.settings_filled,
        labelId = R.string.settings,
        direction = SettingsMobileScreenDestination
    ),
    About(
        iconId = R.drawable.round_info_24,
        labelId = R.string.about,
        direction = AboutMobileScreenDestination
    )
}