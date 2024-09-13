package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.tmdb.Film

interface HomeScreenTvNavigator : CommonScreenNavigator {
    fun openPlayerScreen(film: Film)
}