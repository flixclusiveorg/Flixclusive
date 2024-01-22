package com.flixclusive.mobile

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.flixclusive.MobileNavGraphs
import com.flixclusive.core.ui.common.navigation.UpdateDialogNavigator
import com.flixclusive.feature.mobile.about.destinations.AboutScreenDestination
import com.flixclusive.feature.mobile.film.FilmScreenNavigator
import com.flixclusive.feature.mobile.film.destinations.FilmScreenDestination
import com.flixclusive.feature.mobile.genre.destinations.GenreScreenDestination
import com.flixclusive.feature.mobile.home.HomeNavigator
import com.flixclusive.feature.mobile.player.PlayerScreenNavigator
import com.flixclusive.feature.mobile.player.destinations.PlayerScreenDestination
import com.flixclusive.feature.mobile.preferences.PreferencesScreenNavigator
import com.flixclusive.feature.mobile.provider.destinations.ProvidersScreenDestination
import com.flixclusive.feature.mobile.recentlyWatched.destinations.RecentlyWatchedScreenDestination
import com.flixclusive.feature.mobile.search.SearchScreenNavigator
import com.flixclusive.feature.mobile.searchExpanded.destinations.SearchExpandedScreenDestination
import com.flixclusive.feature.mobile.seeAll.destinations.SeeAllScreenDestination
import com.flixclusive.feature.mobile.settings.destinations.SettingsScreenDestination
import com.flixclusive.feature.mobile.splashScreen.SplashScreenNavigator
import com.flixclusive.feature.mobile.update.destinations.UpdateDialogDestination
import com.flixclusive.feature.mobile.update.destinations.UpdateScreenDestination
import com.flixclusive.feature.mobile.watchlist.destinations.WatchlistScreenDestination
import com.flixclusive.model.configuration.CategoryItem
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.Genre
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.util.navGraph
import com.flixclusive.util.navigateIfResumed
import com.ramcosta.composedestinations.dynamic.within
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo

internal class MobileAppNavigator(
    private val destination: NavDestination,
    private val navController: NavController,
    private val closeApp: () -> Unit,
) : HomeNavigator, SearchScreenNavigator, PreferencesScreenNavigator, UpdateDialogNavigator, FilmScreenNavigator, SplashScreenNavigator,
    PlayerScreenNavigator {

    override fun goBack() {
        navController.navigateUp()
    }

    override fun openSearchExpandedScreen() {
        navController.navigateIfResumed(SearchExpandedScreenDestination within destination.navGraph())
    }

    override fun openSeeAllScreen(item: CategoryItem) {
        navController.navigateIfResumed(SeeAllScreenDestination(item = item) within destination.navGraph())
    }

    override fun openFilmScreen(film: Film) {
        navController.navigateIfResumed(FilmScreenDestination(film = film) within destination.navGraph())
    }

    override fun openGenreScreen(genre: Genre) {
        navController.navigateIfResumed(GenreScreenDestination(genre = genre) within destination.navGraph())
    }

    override fun openWatchlistScreen() {
        navController.navigateIfResumed(WatchlistScreenDestination within destination.navGraph())
    }

    override fun openRecentlyWatchedScreen() {
        navController.navigateIfResumed(RecentlyWatchedScreenDestination within destination.navGraph())
    }

    override fun openSettingsScreen() {
        navController.navigateIfResumed(SettingsScreenDestination within destination.navGraph())
    }

    override fun openProvidersScreen() {
        navController.navigateIfResumed(ProvidersScreenDestination within destination.navGraph())
    }

    override fun openAboutScreen() {
        navController.navigateIfResumed(AboutScreenDestination within destination.navGraph())
    }

    override fun checkForUpdates() {
        navController.navigateIfResumed(UpdateDialogDestination within destination.navGraph())
    }

    override fun openUpdateScreen(
        newVersion: String,
        updateUrl: String,
        updateInfo: String?
    ) {
        navController.navigateIfResumed(
            UpdateScreenDestination(
                newVersion = newVersion,
                updateUrl = updateUrl,
                updateInfo = updateInfo
            )
        )
    }

    override fun openHomeScreen() {
        navController.navigate(MobileNavGraphs.home) {
            popUpTo(MobileNavGraphs.root) {
                saveState = true
            }

            launchSingleTop = true
            restoreState = true
        }
    }

    override fun onExitApplication() {
        closeApp()
    }

    override fun onEpisodeChange(
        film: Film,
        episodeToPlay: TMDBEpisode
    ) {
        navController.navigate(
            PlayerScreenDestination(
                film = film,
                episodeToPlay = episodeToPlay
            )
        ) {
            launchSingleTop = true
        }
    }
}