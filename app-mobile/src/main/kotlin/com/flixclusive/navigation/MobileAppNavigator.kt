package com.flixclusive.navigation

import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
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
import com.flixclusive.feature.mobile.app.updates.dialog.AppUpdatesDialogNavigator
import com.flixclusive.feature.mobile.app.updates.screen.AppUpdatesScreenNavigator
import com.flixclusive.feature.mobile.film.FilmScreenNavigator
import com.flixclusive.feature.mobile.home.HomeNavigator
import com.flixclusive.feature.mobile.library.details.LibraryDetailsScreenNavigator
import com.flixclusive.feature.mobile.library.manage.ManageLibraryScreenNavigator
import com.flixclusive.feature.mobile.profiles.UserProfilesScreenNavigator
import com.flixclusive.feature.mobile.provider.add.AddProviderScreenNavigator
import com.flixclusive.feature.mobile.provider.details.ProviderDetailsNavigator
import com.flixclusive.feature.mobile.provider.manage.ProviderManagerScreenNavigator
import com.flixclusive.feature.mobile.search.SearchScreenNavigator
import com.flixclusive.feature.mobile.searchExpanded.SearchExpandedScreenNavigator
import com.flixclusive.feature.mobile.seeAll.SeeAllScreenNavigator
import com.flixclusive.feature.mobile.settings.screen.root.SettingsScreenNavigator
import com.flixclusive.feature.mobile.onboarding.OnboardingScreenNavigator
import com.flixclusive.feature.mobile.user.add.AddUserScreenNavigator
import com.flixclusive.feature.mobile.user.edit.UserEditScreenNavigator
import com.flixclusive.feature.splashScreen.SplashScreenNavigator
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository
import com.flixclusive.navigation.extensions.navGraph
import com.ramcosta.composedestinations.generated.appmobile.destinations.AppAppLevelMarkdownScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.HomeAppLevelFilmScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.HomeAppLevelSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.LibraryAppLevelFilmScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.LibraryAppLevelSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SearchAppLevelFilmScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SearchAppLevelSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SettingsAppLevelMarkdownScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.AppGraph
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.HomeGraph
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.LibraryGraph
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.SearchGraph
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.SettingsGraph
import com.ramcosta.composedestinations.generated.appupdates.destinations.AppUpdatesScreenDestination
import com.ramcosta.composedestinations.generated.librarydetails.destinations.LibraryDetailsScreenDestination
import com.ramcosta.composedestinations.generated.profiles.destinations.UserProfilesScreenDestination
import com.ramcosta.composedestinations.generated.provideradd.destinations.AddProviderScreenDestination
import com.ramcosta.composedestinations.generated.providerdetails.destinations.ProviderDetailsScreenDestination
import com.ramcosta.composedestinations.generated.providermanage.destinations.ProviderManagerScreenDestination
import com.ramcosta.composedestinations.generated.providersettings.destinations.ProviderSettingsScreenDestination
import com.ramcosta.composedestinations.generated.providertest.destinations.ProviderTestScreenDestination
import com.ramcosta.composedestinations.generated.repositorymanage.destinations.RepositoryManagerScreenDestination
import com.ramcosta.composedestinations.generated.searchexpanded.destinations.SearchExpandedScreenDestination
import com.ramcosta.composedestinations.generated.onboarding.destinations.OnboardingScreenDestination
import com.ramcosta.composedestinations.generated.useradd.destinations.AddUserScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.PinSetupScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.PinVerifyScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.UserAvatarSelectScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.UserEditScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

internal class MobileAppNavigator(
    private val lifecycleOwner: LifecycleOwner,
    private val destination: NavDestination,
    private val navigator: DestinationsNavigator,
    private val uriHandler: UriHandler,
    private val exitAction: ExitAction,
    private val previewFilmAction: ViewFilmPreviewAction,
    private val startPlayerAction: StartPlayerAction,
) : AddProfileAction,
    AddProviderScreenNavigator,
    AddUserScreenNavigator,
    OnboardingScreenNavigator,
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
    private val currentNavGraph get() = destination.navGraph()

    private fun runOnResumed(
        navigationAction: () -> Unit,
    ) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            navigationAction()
        }
    }

    override fun goBack() {
        navigator.navigateUp()
    }

    override fun openSearchExpandedScreen() {
        runOnResumed {
            navigator.navigate(SearchExpandedScreenDestination)
        }
    }

    override fun openSeeAllScreen(item: Catalog) {
        runOnResumed {
            when (currentNavGraph) {
                is HomeGraph -> navigator.navigate(HomeAppLevelSeeAllScreenDestination(catalog = item))
                is SearchGraph -> navigator.navigate(SearchAppLevelSeeAllScreenDestination(catalog = item))
                is LibraryGraph -> navigator.navigate(LibraryAppLevelSeeAllScreenDestination(catalog = item))
            }
        }
    }

    override fun openFilmScreen(film: Film) {
        runOnResumed {
            when (currentNavGraph) {
                is HomeGraph -> navigator.navigate(HomeAppLevelFilmScreenDestination(film = film, isTogglingLibrary = false))
                is SearchGraph -> navigator.navigate(SearchAppLevelFilmScreenDestination(film = film, isTogglingLibrary = false))
                is LibraryGraph -> navigator.navigate(LibraryAppLevelFilmScreenDestination(film = film, isTogglingLibrary = false))
            }
        }
    }

    override fun openLibraryDetails(list: LibraryList) {
        runOnResumed {
            navigator.navigate(LibraryDetailsScreenDestination(list))
        }
    }

    override fun openUpdateScreen(
        newVersion: String,
        updateUrl: String,
        updateInfo: String?,
        isComingFromSplashScreen: Boolean,
    ) {
        runOnResumed {
            navigator.navigate(
                AppUpdatesScreenDestination(
                    newVersion = newVersion,
                    updateUrl = updateUrl,
                    updateInfo = updateInfo,
                    isComingFromSplashScreen = isComingFromSplashScreen,
                ),
            )
        }
    }

    override fun openHomeScreen() {
        runOnResumed {
            navigator.navigate(HomeGraph) {
                popUpTo(AppGraph) {
                    saveState = true
                }

                launchSingleTop = true
                restoreState = true
            }
        }
    }

    override fun openProfilesScreen(shouldPopBackStack: Boolean) {
        runOnResumed {
            navigator.navigate(
                UserProfilesScreenDestination(isFromSplashScreen = shouldPopBackStack),
            ) {
                if (shouldPopBackStack) {
                    popUpTo(AppGraph) {
                        inclusive = true
                    }
                }
            }
        }
    }

    override fun openOnboardingScreen() {
        runOnResumed {
            navigator.navigate(OnboardingScreenDestination) {
                popUpTo(AppGraph) {
                    inclusive = true
                }
            }
        }
    }

    override fun openUserAvatarSelectScreen(selected: Int) {
        runOnResumed {
            navigator.navigate(
                UserAvatarSelectScreenDestination(selected = selected),
            )
        }
    }

    override fun openUserPinScreen(action: PinAction) {
        val destination =
            when (action) {
                is PinAction.Setup -> PinSetupScreenDestination()
                is PinAction.Verify -> PinVerifyScreenDestination(actualPin = action.userPin)
            }

        runOnResumed {
            navigator.navigate(destination)
        }
    }

    override fun openAddProfileScreen(isInitializing: Boolean) {
        runOnResumed {
            navigator.navigate(AddUserScreenDestination(isInitializing = isInitializing))
        }
    }

    override fun onExitApplication() {
        exitAction.onExitApplication()
    }

    override fun openProviderSettings(providerMetadata: ProviderMetadata) {
        runOnResumed {
            navigator.navigate(
                ProviderSettingsScreenDestination(
                    metadata = providerMetadata,
                ),
            )
        }
    }

    override fun openRepositoryManagerScreen() {
        runOnResumed {
            navigator.navigate(
                RepositoryManagerScreenDestination,
            )
        }
    }

    override fun testProviders(providers: ArrayList<ProviderMetadata>) {
        runOnResumed {
            navigator.navigate(
                ProviderTestScreenDestination(providers = providers),
            )
        }
    }

    override fun openProviderDetails(providerMetadata: ProviderMetadata) {
        runOnResumed {
            navigator.navigate(
                ProviderDetailsScreenDestination(metadata = providerMetadata),
            )
        }
    }

    override fun openMarkdownScreen(
        title: String,
        description: String,
    ) {
        val direction = when (currentNavGraph) {
            is HomeGraph -> AppAppLevelMarkdownScreenDestination(title = title, description = description)
            is SettingsGraph -> SettingsAppLevelMarkdownScreenDestination(title = title, description = description)
            else -> throw IllegalStateException("Markdown screen can only be opened from Home or Settings graph")
        }

        runOnResumed {
            navigator.navigate(direction)
        }
    }

    override fun openProviderManagerScreen() {
        runOnResumed {
            navigator.navigate(ProviderManagerScreenDestination)
        }
    }

    override fun openLink(url: String) {
        uriHandler.openUri(url)
    }

    override fun openEditUserScreen(userId: String) {
        runOnResumed {
            navigator.navigate(UserEditScreenDestination(userId = userId))
        }
    }

    override fun previewFilm(film: Film) {
        previewFilmAction.previewFilm(film)
    }

    override fun play(film: Film, episode: Episode?) {
        startPlayerAction.play(film, episode)
    }

    override fun openAddProviderScreen(initialSelectedRepositoryFilter: Repository?) {
        runOnResumed {
            navigator.navigate(
                AddProviderScreenDestination(initialSelectedRepositoryFilter = initialSelectedRepositoryFilter),
            )
        }
    }
}
