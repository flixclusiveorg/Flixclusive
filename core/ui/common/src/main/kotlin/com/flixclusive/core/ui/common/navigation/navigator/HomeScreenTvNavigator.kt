package com.flixclusive.core.ui.common.navigation.navigator

import com.flixclusive.model.film.Film

interface HomeScreenTvNavigator : CommonScreenNavigator {
    fun openPlayerScreen(film: Film)
}