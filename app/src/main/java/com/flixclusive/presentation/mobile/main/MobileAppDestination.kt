package com.flixclusive.presentation.mobile.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.flixclusive.R
import com.flixclusive.presentation.NavGraphs
import com.flixclusive.presentation.destinations.HomeMobileScreenDestination
import com.flixclusive.presentation.destinations.PreferencesMobileScreenDestination
import com.flixclusive.presentation.destinations.SearchMobileScreenDestination
import com.ramcosta.composedestinations.spec.DirectionDestinationSpec
import com.ramcosta.composedestinations.spec.NavGraphSpec

enum class MobileAppDestination(
    val direction: DirectionDestinationSpec,
    val navGraph: NavGraphSpec,
    @DrawableRes val iconSelected: Int,
    @DrawableRes val iconUnselected: Int,
    @StringRes val label: Int
) {
    Home(
        direction = HomeMobileScreenDestination,
        navGraph = NavGraphs.home,
        iconSelected = R.drawable.home,
        iconUnselected = R.drawable.home_outlined,
        label = R.string.home
    ),
    Search(
        direction = SearchMobileScreenDestination,
        navGraph = NavGraphs.search,
        iconSelected = R.drawable.search,
        iconUnselected = R.drawable.search_outlined,
        label = R.string.search
    ),
    Preferences(
        direction = PreferencesMobileScreenDestination,
        navGraph = NavGraphs.preferences,
        iconSelected = R.drawable.settings_filled,
        iconUnselected = R.drawable.settings,
        label = R.string.preferences
    ),
}