package com.flixclusive.mobile

import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.navigation.navargs.GenreWithBackdrop
import com.flixclusive.core.navigation.navigator.AddProfileAction
import com.flixclusive.core.navigation.navigator.ChooseProfileAction
import com.flixclusive.core.navigation.navigator.EditUserAction
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.OpenPinScreenAction
import com.flixclusive.core.navigation.navigator.PinAction
import com.flixclusive.core.navigation.navigator.SelectAvatarAction
import com.flixclusive.core.navigation.navigator.SplashScreenNavigator
import com.flixclusive.core.navigation.navigator.TestProvidersAction
import com.flixclusive.core.navigation.navigator.ViewAllFilmsAction
import com.flixclusive.core.navigation.navigator.ViewFilmAction
import com.flixclusive.core.navigation.navigator.ViewGenreCatalogAction
import com.flixclusive.core.navigation.navigator.ViewMarkdownAction
import com.flixclusive.core.navigation.navigator.ViewNewAppUpdatesAction
import com.flixclusive.core.navigation.navigator.ViewProviderAction
import com.flixclusive.core.navigation.navigator.ViewRepositoryAction
import com.flixclusive.feature.mobile.film.FilmScreenNavigator
import com.flixclusive.feature.mobile.film.destinations.FilmScreenDestination
import com.flixclusive.feature.mobile.genre.GenreScreenNavigator
import com.flixclusive.feature.mobile.genre.destinations.GenreScreenDestination
import com.flixclusive.feature.mobile.home.HomeNavigator
import com.flixclusive.feature.mobile.library.details.LibraryDetailsScreenNavigator
import com.flixclusive.feature.mobile.library.manage.ManageLibraryScreenNavigator
import com.flixclusive.feature.mobile.markdown.destinations.MarkdownScreenDestination
import com.flixclusive.feature.mobile.player.PlayerScreenNavigator
import com.flixclusive.feature.mobile.player.destinations.PlayerScreenDestination
import com.flixclusive.feature.mobile.profiles.UserProfilesScreenNavigator
import com.flixclusive.feature.mobile.profiles.destinations.UserProfilesScreenDestination
import com.flixclusive.feature.mobile.provider.add.AddProviderScreenNavigator
import com.flixclusive.feature.mobile.provider.add.destinations.AddProviderScreenDestination
import com.flixclusive.feature.mobile.provider.details.ProviderDetailsNavigator
import com.flixclusive.feature.mobile.provider.details.destinations.ProviderDetailsScreenDestination
import com.flixclusive.feature.mobile.provider.manage.ProviderManagerScreenNavigator
import com.flixclusive.feature.mobile.provider.manage.destinations.ProviderManagerScreenDestination
import com.flixclusive.feature.mobile.provider.settings.destinations.ProviderSettingsScreenDestination
import com.flixclusive.feature.mobile.provider.test.destinations.ProviderTestScreenDestination
import com.flixclusive.feature.mobile.repository.details.destinations.RepositoryDetailsScreenDestination
import com.flixclusive.feature.mobile.repository.manage.RepositoryManagerScreenNavigator
import com.flixclusive.feature.mobile.repository.manage.destinations.RepositoryManagerScreenDestination
import com.flixclusive.feature.mobile.search.SearchScreenNavigator
import com.flixclusive.feature.mobile.searchExpanded.SearchExpandedScreenNavigator
import com.flixclusive.feature.mobile.searchExpanded.destinations.SearchExpandedScreenDestination
import com.flixclusive.feature.mobile.seeAll.SeeAllScreenNavigator
import com.flixclusive.feature.mobile.seeAll.destinations.SeeAllScreenDestination
import com.flixclusive.feature.mobile.settings.screen.root.SettingsScreenNavigator
import com.flixclusive.feature.mobile.update.UpdateDialogNavigator
import com.flixclusive.feature.mobile.update.UpdateScreenNavigator
import com.flixclusive.feature.mobile.update.destinations.UpdateScreenDestination
import com.flixclusive.feature.mobile.user.add.AddUserScreenNavigator
import com.flixclusive.feature.mobile.user.add.destinations.AddUserScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinSetupScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinVerifyScreenDestination
import com.flixclusive.feature.mobile.user.destinations.UserAvatarSelectScreenDestination
import com.flixclusive.feature.mobile.user.destinations.UserEditScreenDestination
import com.flixclusive.feature.mobile.user.edit.UserEditScreenNavigator
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository
import com.flixclusive.util.navGraph
import com.flixclusive.util.navigateIfResumed
import com.ramcosta.composedestinations.dynamic.within
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo

internal class MobileAppNavigator(
    private val destination: NavDestination,
    private val navController: NavController,
    private val uriHandler: UriHandler,
    private val closeApp: () -> Unit,
) : AddProfileAction,
    AddProviderScreenNavigator,
    AddUserScreenNavigator,
    ChooseProfileAction,
    EditUserAction,
    FilmScreenNavigator,
    GenreScreenNavigator,
    GoBackAction,
    HomeNavigator,
    LibraryDetailsScreenNavigator,
    ManageLibraryScreenNavigator,
    OpenPinScreenAction,
    PlayerScreenNavigator,
    ProviderDetailsNavigator,
    ProviderManagerScreenNavigator,
    RepositoryManagerScreenNavigator,
    SearchExpandedScreenNavigator,
    SearchScreenNavigator,
    SeeAllScreenNavigator,
    SelectAvatarAction,
    SettingsScreenNavigator,
    SplashScreenNavigator,
    TestProvidersAction,
    UpdateDialogNavigator,
    UpdateScreenNavigator,
    UserEditScreenNavigator,
    UserProfilesScreenNavigator,
    ViewAllFilmsAction,
    ViewFilmAction,
    ViewGenreCatalogAction,
    ViewMarkdownAction,
    ViewNewAppUpdatesAction,
    ViewProviderAction,
    ViewRepositoryAction {
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
                startPlayerAutomatically = false,
            ) within destination.navGraph(),
        )
    }

    override fun openGenreScreen(genre: GenreWithBackdrop) {
        navController.navigateIfResumed(GenreScreenDestination(genre = genre) within destination.navGraph())
    }

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
//    override fun openAboutScreen() {
//        navController.navigateIfResumed(AboutScreenDestination within destination.navGraph())
//    }
//
//    override fun checkForUpdates() {
//        navController.navigateIfResumed(UpdateDialogDestination within destination.navGraph())
//    }

    override fun openLibraryDetails(list: LibraryList) {
        navController.navigateIfResumed(
            LibraryDetailsScreenDestination(list) within destination.navGraph()
        )
    }

    override fun openUpdateScreen(
        newVersion: String,
        updateUrl: String,
        updateInfo: String?,
        isComingFromSplashScreen: Boolean,
    ) {
        navController.navigateIfResumed(
            UpdateScreenDestination(
                newVersion = newVersion,
                updateUrl = updateUrl,
                updateInfo = updateInfo,
                isComingFromSplashScreen = isComingFromSplashScreen,
            ),
        )
    }

    override fun openHomeScreen() {
        navController.navigate(MobileNavGraphs.home) {
            popUpTo(MobileNavGraphs.root) {
                inclusive = true
            }
        }
    }

    override fun openProfilesScreen(shouldPopBackStack: Boolean) {
        navController.navigateIfResumed(
            UserProfilesScreenDestination(isFromSplashScreen = shouldPopBackStack),
        ) {
            if (shouldPopBackStack) {
                popUpTo(MobileNavGraphs.root) {
                    inclusive = true
                }
            }
        }
    }

    override fun openUserAvatarSelectScreen(selected: Int) {
        navController.navigateIfResumed(
            UserAvatarSelectScreenDestination(selected = selected),
        )
    }

    override fun openUserPinScreen(action: PinAction) {
        val destination =
            when (action) {
                is PinAction.Setup -> PinSetupScreenDestination()
                is PinAction.Verify -> PinVerifyScreenDestination(user = action.user)
            }

        navController.navigateIfResumed(destination)
    }

    override fun openEditUserScreen(user: User) {
        navController.navigateIfResumed(UserEditScreenDestination(userArg = user))
    }

    override fun openAddProfileScreen(isInitializing: Boolean) {
        navController.navigateIfResumed(AddUserScreenDestination(isInitializing = isInitializing)) {
            if (isInitializing) {
                popUpTo(MobileNavGraphs.root) {
                    saveState = true
                }

                launchSingleTop = true
                restoreState = true
            }
        }
    }

    override fun onExitApplication() {
        closeApp()
    }

    override fun onEpisodeChange(
        film: Film,
        episodeToPlay: Episode,
    ) {
        navController.navigate(
            PlayerScreenDestination(
                film = film,
                episodeToPlay = episodeToPlay,
            ),
        ) {
            launchSingleTop = true
        }
    }

    override fun openProviderSettings(providerMetadata: ProviderMetadata) {
        navController.navigateIfResumed(
            ProviderSettingsScreenDestination(providerMetadata = providerMetadata) within destination.navGraph(),
        )
    }

    override fun openRepositoryManagerScreen() {
        navController.navigateIfResumed(
            RepositoryManagerScreenDestination within destination.navGraph(),
        )
    }

    override fun openRepositoryDetails(repository: Repository) {
        navController.navigateIfResumed(
            RepositoryDetailsScreenDestination(repository = repository) within destination.navGraph(),
        )
    }

    override fun openAddProviderScreen() {
        navController.navigateIfResumed(
            AddProviderScreenDestination() within destination.navGraph(),
        )
    }

    override fun testProviders(providers: ArrayList<ProviderMetadata>) {
        navController.navigateIfResumed(
            ProviderTestScreenDestination(providers = providers) within destination.navGraph(),
        )
    }

    override fun openProviderDetails(providerMetadata: ProviderMetadata) {
        navController.navigateIfResumed(
            ProviderDetailsScreenDestination(providerMetadata = providerMetadata) within destination.navGraph(),
        )
    }

    override fun openMarkdownScreen(
        title: String,
        description: String,
    ) {
        navController.navigateIfResumed(
            MarkdownScreenDestination(
                title = title,
                description = description,
            ) within destination.navGraph(),
        )
    }

    override fun openProviderManagerScreen() {
        navController.navigateIfResumed(
            ProviderManagerScreenDestination within destination.navGraph(),
        )
    }

    override fun openLink(url: String) {
        uriHandler.openUri(url)
    }
}
