package com.flixclusive.mobile

import com.flixclusive.ROOT
import com.flixclusive.feature.mobile.about.destinations.AboutScreenDestination
import com.flixclusive.feature.mobile.markdown.destinations.MarkdownScreenDestination
import com.flixclusive.feature.mobile.film.destinations.FilmScreenDestination
import com.flixclusive.feature.mobile.genre.destinations.GenreScreenDestination
import com.flixclusive.feature.mobile.home.destinations.HomeScreenDestination
import com.flixclusive.feature.mobile.player.destinations.PlayerScreenDestination
import com.flixclusive.feature.mobile.preferences.destinations.PreferencesScreenDestination
import com.flixclusive.feature.mobile.provider.destinations.ProvidersScreenDestination
import com.flixclusive.feature.mobile.provider.info.destinations.ProviderInfoScreenDestination
import com.flixclusive.feature.mobile.provider.settings.destinations.ProviderSettingsScreenDestination
import com.flixclusive.feature.mobile.provider.test.destinations.ProviderTestScreenDestination
import com.flixclusive.feature.mobile.recentlyWatched.destinations.RecentlyWatchedScreenDestination
import com.flixclusive.feature.mobile.repository.destinations.RepositoryScreenDestination
import com.flixclusive.feature.mobile.repository.search.destinations.RepositorySearchScreenDestination
import com.flixclusive.feature.mobile.search.destinations.SearchScreenDestination
import com.flixclusive.feature.mobile.searchExpanded.destinations.SearchExpandedScreenDestination
import com.flixclusive.feature.mobile.seeAll.destinations.SeeAllScreenDestination
import com.flixclusive.feature.mobile.settings.destinations.SettingsScreenDestination
import com.flixclusive.feature.mobile.update.destinations.UpdateDialogDestination
import com.flixclusive.feature.mobile.update.destinations.UpdateScreenDestination
import com.flixclusive.feature.mobile.watchlist.destinations.WatchlistScreenDestination
import com.flixclusive.feature.splashScreen.destinations.SplashScreenDestination
import com.ramcosta.composedestinations.dynamic.routedIn
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.NavGraphSpec

internal object MobileNavGraphs {

    val home = object : NavGraphSpec {
        override val route = "home"

        override val startRoute = HomeScreenDestination routedIn this

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            HomeScreenDestination,
            FilmScreenDestination,
            GenreScreenDestination,
            SeeAllScreenDestination
        ).routedIn(this)
            .associateBy { it.route }
    }

    val search = object : NavGraphSpec {
        override val route = "search"

        override val startRoute = SearchScreenDestination routedIn this

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            SearchScreenDestination,
            SearchExpandedScreenDestination,
            FilmScreenDestination,
            GenreScreenDestination,
            SeeAllScreenDestination
        ).routedIn(this)
            .associateBy { it.route }
    }

    val providers = object : NavGraphSpec {
        override val route = "providers"

        override val startRoute = ProvidersScreenDestination routedIn this

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            ProvidersScreenDestination,
            RepositorySearchScreenDestination,
            RepositoryScreenDestination,
            ProviderInfoScreenDestination,
            ProviderSettingsScreenDestination,
            MarkdownScreenDestination,
            ProviderTestScreenDestination
        ).routedIn(this)
            .associateBy { it.route }
    }

    val preferences = object : NavGraphSpec {
        override val route = "preferences"

        override val startRoute = PreferencesScreenDestination routedIn this

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            PreferencesScreenDestination,
            AboutScreenDestination,
            FilmScreenDestination,
            GenreScreenDestination,
            RecentlyWatchedScreenDestination,
            SettingsScreenDestination,
            UpdateDialogDestination,
            WatchlistScreenDestination
        ).routedIn(this)
            .associateBy { it.route }
    }

    val root = object : NavGraphSpec {
        override val route = ROOT

        override val startRoute = SplashScreenDestination

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            PlayerScreenDestination,
            SplashScreenDestination,
            UpdateScreenDestination,
            MarkdownScreenDestination,
        ).associateBy { it.route }

        override val nestedNavGraphs = listOf(
            home,
            search,
            providers,
            preferences,
        )
    }
}