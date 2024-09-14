package com.flixclusive.tv

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.flixclusive.core.ui.common.navigation.navigator.CommonScreenNavigator
import com.flixclusive.core.ui.common.navigation.navigator.FilmScreenTvNavigator
import com.flixclusive.core.ui.common.navigation.navigator.HomeScreenTvNavigator
import com.flixclusive.core.ui.common.navigation.navigator.SplashScreenNavigator
import com.flixclusive.feature.mobile.update.destinations.UpdateScreenDestination
import com.flixclusive.feature.tv.film.destinations.FilmScreenDestination
import com.flixclusive.model.film.Film
import com.flixclusive.util.navGraph
import com.flixclusive.util.navigateIfResumed
import com.ramcosta.composedestinations.dynamic.within
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo

internal class AppTvNavigator(
    private val destination: NavDestination,
    private val navController: NavController,
    private val closeApp: () -> Unit,
) : CommonScreenNavigator, SplashScreenNavigator, FilmScreenTvNavigator, HomeScreenTvNavigator {
    override fun goBack() {
        navController.navigateUp()
    }

    override fun openFilmScreen(film: Film) {
        navController.navigateIfResumed(
            FilmScreenDestination(
                film = film,
                startPlayerAutomatically = false
            ) within destination.navGraph())
    }

    override fun openFilmScreenSeamlessly(film: Film) {
        navController.navigateIfResumed(
            FilmScreenDestination(
                film = film,
                startPlayerAutomatically = false
            ) within destination.navGraph()
        ) {
            launchSingleTop = true
        }
    }

    override fun openPlayerScreen(film: Film) {
        navController.navigateIfResumed(FilmScreenDestination(
            film = film,
            startPlayerAutomatically = true
        ) within destination.navGraph())
    }

    //    override fun openSearchExpandedScreen() {
//        navController.navigateIfResumed(SearchExpandedScreenDestination within destination.navGraph())
//    }
//
//    override fun openWatchlistScreen() {
//        navController.navigateIfResumed(WatchlistScreenDestination within destination.navGraph())
//    }
//
//    override fun openRecentlyWatchedScreen() {
//        navController.navigateIfResumed(RecentlyWatchedScreenDestination within destination.navGraph())
//    }
//
//    override fun openSettingsScreen() {
//        navController.navigateIfResumed(SettingsScreenDestination within destination.navGraph())
//    }
//
//    override fun openProvidersScreen() {
//        navController.navigateIfResumed(PluginsScreenDestination within destination.navGraph())
//    }
//
//    override fun openAboutScreen() {
//        navController.navigateIfResumed(AboutScreenDestination within destination.navGraph())
//    }
//
//    override fun checkForUpdates() {
//        navController.navigateIfResumed(UpdateDialogDestination within destination.navGraph())
//    }

    override fun openUpdateScreen(
        newVersion: String,
        updateUrl: String,
        updateInfo: String?,
        isComingFromSplashScreen: Boolean
    ) {
        navController.navigateIfResumed(
            UpdateScreenDestination(
                newVersion = newVersion,
                updateUrl = updateUrl,
                updateInfo = updateInfo,
                isComingFromSplashScreen = false
            )
        )
    }

    override fun openHomeScreen() {
        navController.navigate(TvNavGraphs.home) {
            popUpTo(TvNavGraphs.root) {
                saveState = true
            }

            launchSingleTop = true
            restoreState = true
        }
    }

    override fun onExitApplication() {
        closeApp()
    }
}