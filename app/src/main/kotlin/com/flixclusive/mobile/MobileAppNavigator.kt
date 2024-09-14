package com.flixclusive.mobile

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.flixclusive.core.ui.common.navigation.navargs.MarkdownNavigator
import com.flixclusive.core.ui.common.navigation.navigator.FilmScreenNavigator
import com.flixclusive.core.ui.common.navigation.navigator.HomeNavigator
import com.flixclusive.core.ui.common.navigation.navigator.PlayerScreenNavigator
import com.flixclusive.core.ui.common.navigation.navigator.PreferencesScreenNavigator
import com.flixclusive.core.ui.common.navigation.navigator.ProviderInfoNavigator
import com.flixclusive.core.ui.common.navigation.navigator.ProviderTestNavigator
import com.flixclusive.core.ui.common.navigation.navigator.ProvidersScreenNavigator
import com.flixclusive.core.ui.common.navigation.navigator.RepositorySearchScreenNavigator
import com.flixclusive.core.ui.common.navigation.navigator.SearchScreenNavigator
import com.flixclusive.core.ui.common.navigation.navigator.SplashScreenNavigator
import com.flixclusive.core.ui.common.navigation.navigator.UpdateDialogNavigator
import com.flixclusive.feature.mobile.about.destinations.AboutScreenDestination
import com.flixclusive.feature.mobile.film.destinations.FilmScreenDestination
import com.flixclusive.feature.mobile.genre.destinations.GenreScreenDestination
import com.flixclusive.feature.mobile.markdown.destinations.MarkdownScreenDestination
import com.flixclusive.feature.mobile.player.destinations.PlayerScreenDestination
import com.flixclusive.feature.mobile.provider.info.destinations.ProviderInfoScreenDestination
import com.flixclusive.feature.mobile.provider.settings.destinations.ProviderSettingsScreenDestination
import com.flixclusive.feature.mobile.provider.test.destinations.ProviderTestScreenDestination
import com.flixclusive.feature.mobile.recentlyWatched.destinations.RecentlyWatchedScreenDestination
import com.flixclusive.feature.mobile.repository.destinations.RepositoryScreenDestination
import com.flixclusive.feature.mobile.repository.search.destinations.RepositorySearchScreenDestination
import com.flixclusive.feature.mobile.searchExpanded.destinations.SearchExpandedScreenDestination
import com.flixclusive.feature.mobile.seeAll.destinations.SeeAllScreenDestination
import com.flixclusive.feature.mobile.settings.destinations.SettingsScreenDestination
import com.flixclusive.feature.mobile.update.destinations.UpdateDialogDestination
import com.flixclusive.feature.mobile.update.destinations.UpdateScreenDestination
import com.flixclusive.feature.mobile.watchlist.destinations.WatchlistScreenDestination
import com.flixclusive.model.provider.ProviderData
import com.flixclusive.model.provider.Repository
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.Genre
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.util.navGraph
import com.flixclusive.util.navigateIfResumed
import com.ramcosta.composedestinations.dynamic.within
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo

internal class MobileAppNavigator(
    private val destination: NavDestination,
    private val navController: NavController,
    private val closeApp: () -> Unit,
) : HomeNavigator, SearchScreenNavigator, PreferencesScreenNavigator, UpdateDialogNavigator,
    FilmScreenNavigator, SplashScreenNavigator, PlayerScreenNavigator, ProvidersScreenNavigator,
    RepositorySearchScreenNavigator, ProviderInfoNavigator, ProviderTestNavigator,
    MarkdownNavigator {

    override fun goBack() {
        navController.navigateUp()
    }

    override fun openSearchExpandedScreen() {
        navController.navigateIfResumed(SearchExpandedScreenDestination within destination.navGraph())
    }

    override fun openSeeAllScreen(item: Catalog) {
        navController.navigateIfResumed(SeeAllScreenDestination(item = item) within destination.navGraph())
    }

    override fun openFilmScreen(film: Film) {
        navController.navigateIfResumed(
            FilmScreenDestination(
                film = film,
                startPlayerAutomatically = false
            ) within destination.navGraph()
        )
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

    override fun openAboutScreen() {
        navController.navigateIfResumed(AboutScreenDestination within destination.navGraph())
    }

    override fun checkForUpdates() {
        navController.navigateIfResumed(UpdateDialogDestination within destination.navGraph())
    }

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
                isComingFromSplashScreen = isComingFromSplashScreen
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
        episodeToPlay: Episode
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

    override fun openProviderSettings(providerData: ProviderData) {
        navController.navigateIfResumed(
            ProviderSettingsScreenDestination(providerData = providerData) within destination.navGraph()
        )
    }

    override fun openAddRepositoryScreen() {
        navController.navigateIfResumed(
            RepositorySearchScreenDestination within destination.navGraph()
        )
    }

    override fun openRepositoryScreen(repository: Repository) {
        navController.navigateIfResumed(
            RepositoryScreenDestination(repository = repository) within destination.navGraph()
        )
    }

    override fun testProviders(providers: ArrayList<ProviderData>) {
        navController.navigateIfResumed(
            ProviderTestScreenDestination(providers = providers) within destination.navGraph()
        )
    }

    override fun seeWhatsNew(providerData: ProviderData) {
        openMarkdownScreen(
            title = providerData.name,
            description = providerData.changelog ?: ""
        )
    }

    override fun openProviderInfo(providerData: ProviderData) {
        navController.navigateIfResumed(
            ProviderInfoScreenDestination(providerData = providerData) within destination.navGraph()
        )
    }

    override fun openMarkdownScreen(title: String, description: String) {
        navController.navigateIfResumed(
            MarkdownScreenDestination(
                title = title,
                description = description
            ) within destination.navGraph()
        )
    }
}