package com.flixclusive.tv

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.flixclusive.core.ui.common.navigation.CommonScreenNavigator
import com.flixclusive.feature.mobile.film.destinations.FilmScreenDestination
import com.flixclusive.feature.mobile.player.PlayerScreenNavigator
import com.flixclusive.feature.mobile.player.destinations.PlayerScreenDestination
import com.flixclusive.feature.mobile.update.destinations.UpdateScreenDestination
import com.flixclusive.feature.splashScreen.SplashScreenNavigator
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.util.navGraph
import com.flixclusive.util.navigateIfResumed
import com.ramcosta.composedestinations.dynamic.within
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo

internal class TvAppNavigator(
    private val destination: NavDestination,
    private val navController: NavController,
    private val closeApp: () -> Unit,
) : CommonScreenNavigator, SplashScreenNavigator, PlayerScreenNavigator {
    override fun goBack() {
        navController.navigateUp()
    }

    override fun openFilmScreen(film: Film) {
        navController.navigateIfResumed(FilmScreenDestination(film = film) within destination.navGraph())
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
//        navController.navigateIfResumed(ProvidersScreenDestination within destination.navGraph())
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