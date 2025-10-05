package com.flixclusive.navigation

import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.navigation.navigator.AddProfileAction
import com.flixclusive.core.navigation.navigator.ChooseProfileAction
import com.flixclusive.core.navigation.navigator.EditUserAction
import com.flixclusive.core.navigation.navigator.ExitAction
import com.flixclusive.core.navigation.navigator.GoBackAction
import com.flixclusive.core.navigation.navigator.OpenPinScreenAction
import com.flixclusive.core.navigation.navigator.PinAction
import com.flixclusive.core.navigation.navigator.SelectAvatarAction
import com.flixclusive.core.navigation.navigator.StartPlayerAction
import com.flixclusive.core.navigation.navigator.TestProvidersAction
import com.flixclusive.core.navigation.navigator.ViewAllFilmsAction
import com.flixclusive.core.navigation.navigator.ViewFilmAction
import com.flixclusive.core.navigation.navigator.ViewFilmPreviewAction
import com.flixclusive.core.navigation.navigator.ViewMarkdownAction
import com.flixclusive.core.navigation.navigator.ViewNewAppUpdatesAction
import com.flixclusive.core.navigation.navigator.ViewProviderAction
import com.flixclusive.feature.mobile.app.updates.destinations.AppUpdatesScreenDestination
import com.flixclusive.feature.mobile.app.updates.dialog.AppUpdatesDialogNavigator
import com.flixclusive.feature.mobile.app.updates.screen.AppUpdatesScreenNavigator
import com.flixclusive.feature.mobile.film.FilmScreenNavigator
import com.flixclusive.feature.mobile.film.destinations.FilmScreenDestination
import com.flixclusive.feature.mobile.home.HomeNavigator
import com.flixclusive.feature.mobile.library.details.LibraryDetailsScreenNavigator
import com.flixclusive.feature.mobile.library.details.destinations.LibraryDetailsScreenDestination
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
import com.flixclusive.feature.mobile.repository.manage.destinations.RepositoryManagerScreenDestination
import com.flixclusive.feature.mobile.search.SearchScreenNavigator
import com.flixclusive.feature.mobile.searchExpanded.SearchExpandedScreenNavigator
import com.flixclusive.feature.mobile.searchExpanded.destinations.SearchExpandedScreenDestination
import com.flixclusive.feature.mobile.seeAll.SeeAllScreenNavigator
import com.flixclusive.feature.mobile.seeAll.destinations.SeeAllScreenDestination
import com.flixclusive.feature.mobile.settings.screen.root.SettingsScreenNavigator
import com.flixclusive.feature.mobile.user.add.AddUserScreenNavigator
import com.flixclusive.feature.mobile.user.add.destinations.AddUserScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinSetupScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinVerifyScreenDestination
import com.flixclusive.feature.mobile.user.destinations.UserAvatarSelectScreenDestination
import com.flixclusive.feature.mobile.user.destinations.UserEditScreenDestination
import com.flixclusive.feature.mobile.user.edit.UserEditScreenNavigator
import com.flixclusive.feature.splashScreen.SplashScreenNavigator
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository
import com.flixclusive.navigation.extensions.navGraph
import com.flixclusive.navigation.extensions.navigateIfResumed
import com.ramcosta.composedestinations.dynamic.within
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.navigation.popUpTo

internal class MobileAppNavigator(
    private val destination: NavDestination,
    private val navController: NavController,
    private val uriHandler: UriHandler,
    private val exitAction: ExitAction,
    private val previewFilmAction: ViewFilmPreviewAction,
    private val startPlayerAction: StartPlayerAction,
) : AddProfileAction,
    AddProviderScreenNavigator,
    AddUserScreenNavigator,
    AppUpdatesDialogNavigator,
    AppUpdatesScreenNavigator,
    ChooseProfileAction,
    EditUserAction,
    ExitAction,
    FilmScreenNavigator,
    GoBackAction,
    HomeNavigator,
    LibraryDetailsScreenNavigator,
    ManageLibraryScreenNavigator,
    OpenPinScreenAction,
    PlayerScreenNavigator,
    ProviderDetailsNavigator,
    ProviderManagerScreenNavigator,
    SearchExpandedScreenNavigator,
    SearchScreenNavigator,
    SeeAllScreenNavigator,
    SelectAvatarAction,
    SettingsScreenNavigator,
    SplashScreenNavigator,
    TestProvidersAction,
    UserEditScreenNavigator,
    UserProfilesScreenNavigator,
    ViewAllFilmsAction,
    ViewFilmAction,
    ViewFilmPreviewAction,
    ViewMarkdownAction,
    ViewNewAppUpdatesAction,
    ViewProviderAction {
    override fun goBack() {
        navController.navigateUp()
    }

    override fun openSearchExpandedScreen() {
        navController.navigateIfResumed(SearchExpandedScreenDestination within destination.navGraph())
    }

    override fun openSeeAllScreen(item: Catalog) {
        navController.navigateIfResumed(SeeAllScreenDestination(catalog = item) within destination.navGraph())
    }

    override fun openFilmScreen(film: Film) {
        navController.navigateIfResumed(
            FilmScreenDestination(film = film) within destination.navGraph(),
        )
    }

    override fun openLibraryDetails(list: LibraryList) {
        navController.navigateIfResumed(LibraryDetailsScreenDestination(list) within destination.navGraph())
    }

    override fun openUpdateScreen(
        newVersion: String,
        updateUrl: String,
        updateInfo: String?,
        isComingFromSplashScreen: Boolean,
    ) {
        navController.navigateIfResumed(
            AppUpdatesScreenDestination(
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
                is PinAction.Verify -> PinVerifyScreenDestination(actualPin = action.userPin)
            }

        navController.navigateIfResumed(destination)
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
        exitAction.onExitApplication()
    }

    override fun onEpisodeChange(film: FilmMetadata, episode: Episode) {
        navController.navigate(
            PlayerScreenDestination(film = film, episode = episode),
        ) {
            launchSingleTop = true
        }
    }

    override fun openProviderSettings(providerMetadata: ProviderMetadata) {
        navController.navigateIfResumed(
            ProviderSettingsScreenDestination(
                metadata = providerMetadata,
            ) within destination.navGraph(),
        )
    }

    override fun openRepositoryManagerScreen() {
        navController.navigateIfResumed(
            RepositoryManagerScreenDestination within destination.navGraph(),
        )
    }

    override fun testProviders(providers: ArrayList<ProviderMetadata>) {
        navController.navigateIfResumed(
            ProviderTestScreenDestination(providers = providers) within destination.navGraph(),
        )
    }

    override fun openProviderDetails(providerMetadata: ProviderMetadata) {
        navController.navigateIfResumed(
            ProviderDetailsScreenDestination(metadata = providerMetadata) within destination.navGraph(),
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

    override fun openEditUserScreen(userId: Int) {
        navController.navigateIfResumed(UserEditScreenDestination(userId = userId))
    }

    override fun previewFilm(film: Film) {
        previewFilmAction.previewFilm(film)
    }

    override fun play(film: Film, episode: Episode?) {
        startPlayerAction.play(film, episode)
    }

    override fun openAddProviderScreen(initialSelectedRepositoryFilter: Repository?) {
        navController.navigateIfResumed(
            AddProviderScreenDestination(initialSelectedRepositoryFilter = initialSelectedRepositoryFilter) within
                destination.navGraph(),
        )
    }
}
