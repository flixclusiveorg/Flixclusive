package com.flixclusive.navigation

import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavDestination
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.navigation.navigator.NavigateToAddProfileScreen
import com.flixclusive.core.navigation.navigator.NavigateToAppUpdatesScreen
import com.flixclusive.core.navigation.navigator.NavigateToChooseProfileScreen
import com.flixclusive.core.navigation.navigator.NavigateToEditUserScreen
import com.flixclusive.core.navigation.navigator.NavigateToManageMediaLinksScreen
import com.flixclusive.core.navigation.navigator.NavigateToMarkdownScreen
import com.flixclusive.core.navigation.navigator.NavigateToMediaImageDialog
import com.flixclusive.core.navigation.navigator.NavigateToMediaLinksBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToMediaPreviewBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToMediaScreen
import com.flixclusive.core.navigation.navigator.NavigateToOpenPinScreen
import com.flixclusive.core.navigation.navigator.NavigateToProviderDetailsBottomSheet
import com.flixclusive.core.navigation.navigator.NavigateToSeeAllScreen
import com.flixclusive.core.navigation.navigator.NavigateToSelectAvatarScreen
import com.flixclusive.core.navigation.navigator.NavigatorExitApp
import com.flixclusive.core.navigation.navigator.PinAction
import com.flixclusive.core.navigation.settings.SubSettingsNavItem
import com.flixclusive.feature.mobile.app.updates.dialog.NavigatorAppUpdatesDialog
import com.flixclusive.feature.mobile.app.updates.screen.NavigatorAppUpdatesScreen
import com.flixclusive.feature.mobile.home.NavigatorHome
import com.flixclusive.feature.mobile.library.details.NavigatorLibraryDetailsScreen
import com.flixclusive.feature.mobile.library.manage.NavigatorManageLibraryScreen
import com.flixclusive.feature.mobile.media.navigator.NavigatorMediaPreviewBottomSheet
import com.flixclusive.feature.mobile.media.navigator.NavigatorMediaScreen
import com.flixclusive.feature.mobile.onboarding.NavigatorOnboardingScreen
import com.flixclusive.feature.mobile.player.NavigatorPlayerSplashScreen
import com.flixclusive.feature.mobile.player.PlayerScreenInitialHeader
import com.flixclusive.feature.mobile.provider.add.NavigatorAddProviderScreen
import com.flixclusive.feature.mobile.provider.details.NavigatorProviderDetailsBottomSheet
import com.flixclusive.feature.mobile.provider.manage.NavigatorProviderManagerScreen
import com.flixclusive.feature.mobile.search.NavigatorSearchScreen
import com.flixclusive.feature.mobile.seeAll.NavigatorSeeAllScreen
import com.flixclusive.feature.mobile.settings.screen.data.NavigatorDataTweakScreen
import com.flixclusive.feature.mobile.settings.screen.links.manage.NavigatorManageMediaLinksTweakScreen
import com.flixclusive.feature.mobile.settings.screen.links.root.NavigatorMediaLinkCardsTweakScreen
import com.flixclusive.feature.mobile.settings.screen.links.show.NavigatorMediaLinksShowDetailTweakScreen
import com.flixclusive.feature.mobile.settings.screen.player.NavigatorPlayerTweakScreen
import com.flixclusive.feature.mobile.settings.screen.providers.NavigatorProvidersTweakScreen
import com.flixclusive.feature.mobile.settings.screen.root.NavigatorSettingsScreen
import com.flixclusive.feature.mobile.user.add.NavigatorAddUserScreenNavigateTo
import com.flixclusive.feature.mobile.user.edit.NavigatorUserEditScreen
import com.flixclusive.feature.mobile.user.profiles.NavigatorUserProfilesScreen
import com.flixclusive.feature.splashScreen.NavigatorSplashScreen
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.provider.Catalog
import com.flixclusive.model.provider.ProviderMetadata
import com.flixclusive.model.provider.Repository
import com.flixclusive.navigation.extensions.navGraph
import com.ramcosta.composedestinations.generated.appmobile.destinations.HomeCommonMediaPreviewBottomSheetDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.HomeCommonMediaScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.HomeCommonSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.LibraryCommonMediaPreviewBottomSheetDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.LibraryCommonMediaScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.LibraryCommonSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SearchCommonMediaPreviewBottomSheetDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SearchCommonMediaScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SearchCommonSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SettingsCommonMediaPreviewBottomSheetDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SettingsCommonMediaScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SettingsCommonSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.AppGraph
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.HomeGraph
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.LibraryGraph
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.SearchGraph
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.SettingsGraph
import com.ramcosta.composedestinations.generated.appupdates.destinations.AppUpdatesScreenDestination
import com.ramcosta.composedestinations.generated.librarydetails.destinations.LibraryDetailsScreenDestination
import com.ramcosta.composedestinations.generated.markdown.destinations.MarkdownScreenDestination
import com.ramcosta.composedestinations.generated.media.destinations.MediaImagePreviewDialogDestination
import com.ramcosta.composedestinations.generated.media.destinations.MediaLinksBottomSheetDestination
import com.ramcosta.composedestinations.generated.onboarding.destinations.OnboardingScreenDestination
import com.ramcosta.composedestinations.generated.player.destinations.PlayerScreenDestination
import com.ramcosta.composedestinations.generated.player.destinations.PlayerSplashScreenDestination
import com.ramcosta.composedestinations.generated.provideradd.destinations.AddProviderScreenDestination
import com.ramcosta.composedestinations.generated.providerdetails.destinations.ProviderDetailsBottomSheetDestination
import com.ramcosta.composedestinations.generated.providermanage.destinations.ProviderManagerScreenDestination
import com.ramcosta.composedestinations.generated.providersettings.destinations.ProviderSettingsScreenDestination
import com.ramcosta.composedestinations.generated.repositorymanage.destinations.RepositoryManagerScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.AppearanceTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.DataTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.ManageMediaLinksTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.MediaLinkCardsTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.MediaLinksShowDetailTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.PlayerTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.ProvidersTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.SubtitlesTweakScreenDestination
import com.ramcosta.composedestinations.generated.settings.destinations.SystemTweakScreenDestination
import com.ramcosta.composedestinations.generated.useradd.destinations.AddUserScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.PinSetupScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.PinVerifyScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.UserAvatarSelectScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.UserEditScreenDestination
import com.ramcosta.composedestinations.generated.userprofiles.destinations.UserProfilesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

internal class MobileAppNavigator(
    private val lifecycleOwner: LifecycleOwner,
    private val destination: NavDestination,
    private val navigator: DestinationsNavigator,
    private val uriHandler: UriHandler,
    private val navigatorExitApp: NavigatorExitApp,
) : NavigateBack,
    NavigateToAddProfileScreen,
    NavigateToAppUpdatesScreen,
    NavigateToChooseProfileScreen,
    NavigateToEditUserScreen,
    NavigateToManageMediaLinksScreen,
    NavigateToMarkdownScreen,
    NavigateToMediaImageDialog,
    NavigateToMediaLinksBottomSheet,
    NavigateToMediaPreviewBottomSheet,
    NavigateToMediaScreen,
    NavigateToOpenPinScreen,
    NavigateToProviderDetailsBottomSheet,
    NavigateToSeeAllScreen,
    NavigateToSelectAvatarScreen,
    NavigatorAddProviderScreen,
    NavigatorAddUserScreenNavigateTo,
    NavigatorAppUpdatesDialog,
    NavigatorAppUpdatesScreen,
    NavigatorDataTweakScreen,
    NavigatorExitApp,
    NavigatorHome,
    NavigatorLibraryDetailsScreen,
    NavigatorManageLibraryScreen,
    NavigatorManageMediaLinksTweakScreen,
    NavigatorMediaLinkCardsTweakScreen,
    NavigatorMediaLinksShowDetailTweakScreen,
    NavigatorMediaPreviewBottomSheet,
    NavigatorMediaScreen,
    NavigatorOnboardingScreen,
    NavigatorPlayerSplashScreen,
    NavigatorPlayerTweakScreen,
    NavigatorProviderDetailsBottomSheet,
    NavigatorProviderManagerScreen,
    NavigatorProvidersTweakScreen,
    NavigatorSearchScreen,
    NavigatorSeeAllScreen,
    NavigatorSettingsScreen,
    NavigatorSplashScreen,
    NavigatorUserEditScreen,
    NavigatorUserProfilesScreen {
    private val currentNavGraph get() = destination.navGraph()

    private fun runOnResumed(
        navigationAction: () -> Unit,
    ) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            navigationAction()
        }
    }

    override fun navigateBack() {
        navigator.navigateUp()
    }

    override fun navigateToSeeAllScreen(item: Catalog) {
        runOnResumed {
            when (currentNavGraph) {
                is HomeGraph -> navigator.navigate(HomeCommonSeeAllScreenDestination(catalog = item))
                is LibraryGraph -> navigator.navigate(LibraryCommonSeeAllScreenDestination(catalog = item))
                is SettingsGraph -> navigator.navigate(SettingsCommonSeeAllScreenDestination(catalog = item))
                is SearchGraph -> navigator.navigate(SearchCommonSeeAllScreenDestination(catalog = item))
            }
        }
    }

    override fun navigateToMediaScreen(media: MediaMetadata, isTogglingLibrary: Boolean) {
        runOnResumed {
            when (currentNavGraph) {
                is HomeGraph -> navigator.navigate(
                    HomeCommonMediaScreenDestination(media = media, isTogglingLibrary = isTogglingLibrary)
                )

                is LibraryGraph -> navigator.navigate(
                    LibraryCommonMediaScreenDestination(media = media, isTogglingLibrary = isTogglingLibrary)
                )

                is SettingsGraph -> navigator.navigate(
                    SettingsCommonMediaScreenDestination(media = media, isTogglingLibrary = isTogglingLibrary)
                )

                is SearchGraph -> navigator.navigate(
                    SearchCommonMediaScreenDestination(media = media, isTogglingLibrary = isTogglingLibrary)
                )
            }
        }
    }

    override fun navigateToLibraryDetailsScreen(list: LibraryList, tracker: ProviderMetadata?) {
        runOnResumed {
            navigator.navigate(LibraryDetailsScreenDestination(list, tracker))
        }
    }

    override fun navigateToAppUpdateScreen(
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

    override fun navigateToHomeScreen() {
        // Not running on RESUMED only
        // there might be cases where users close app even
        // before triggering this method
        navigator.navigate(HomeGraph) {
            popUpTo(AppGraph) {
                saveState = true
            }
        }
    }

    override fun navigateToUserProfilesScreen(shouldPopBackStack: Boolean) {
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

    override fun navigateToOnboardingScreen() {
        runOnResumed {
            navigator.navigate(OnboardingScreenDestination) {
                popUpTo(AppGraph) {
                    inclusive = true
                }
            }
        }
    }

    override fun navigateToUserAvatarSelectScreen(selected: Int) {
        runOnResumed {
            navigator.navigate(
                UserAvatarSelectScreenDestination(selected = selected),
            )
        }
    }

    override fun navigateToUserPinScreen(action: PinAction) {
        val destination =
            when (action) {
                is PinAction.Setup -> PinSetupScreenDestination()
                is PinAction.Verify -> PinVerifyScreenDestination(actualPin = action.userPin)
            }

        runOnResumed {
            navigator.navigate(destination)
        }
    }

    override fun navigateToAddProfileScreen(isInitializing: Boolean) {
        runOnResumed {
            navigator.navigate(AddUserScreenDestination(isInitializing = isInitializing)) {
                if (isInitializing) {
                    popUpTo(AppGraph) {
                        inclusive = true
                    }
                }
            }
        }
    }

    override fun exitApplication() {
        navigatorExitApp.exitApplication()
    }

    override fun navigateToProviderSettings(provider: ProviderMetadata) {
        runOnResumed {
            navigator.navigate(
                ProviderSettingsScreenDestination(id = provider.id),
            )
        }
    }

    override fun navigateToRepositoryManagerScreen() {
        runOnResumed {
            navigator.navigate(
                RepositoryManagerScreenDestination,
            )
        }
    }

    override fun showProviderDetailsSheet(provider: ProviderMetadata) {
        runOnResumed {
            navigator.navigate(
                ProviderDetailsBottomSheetDestination(metadata = provider),
            )
        }
    }

    override fun navigateToMarkdownScreen(
        title: String,
        description: String,
    ) {
        runOnResumed {
            navigator.navigate(MarkdownScreenDestination(title = title, description = description))
        }
    }

    override fun navigateToProviderManagerScreen() {
        runOnResumed {
            navigator.navigate(ProviderManagerScreenDestination)
        }
    }

    override fun navigateToUrl(url: String) {
        uriHandler.openUri(url)
    }

    override fun navigateToEditUserScreen(userId: String) {
        runOnResumed {
            navigator.navigate(UserEditScreenDestination(userId = userId))
        }
    }

    override fun showMediaPreviewBottomSheet(media: MediaMetadata) {
        runOnResumed {
            when (currentNavGraph) {
                is HomeGraph -> navigator.navigate(
                    HomeCommonMediaPreviewBottomSheetDestination(media = media)
                )

                is LibraryGraph -> navigator.navigate(
                    LibraryCommonMediaPreviewBottomSheetDestination(media = media)
                )

                is SearchGraph -> navigator.navigate(
                    SearchCommonMediaPreviewBottomSheetDestination(media = media)
                )

                is SettingsGraph -> navigator.navigate(
                    SettingsCommonMediaPreviewBottomSheetDestination(media = media)
                )
            }
        }
    }

    override fun showMediaImageDialog(imagePath: String) {
        runOnResumed {
            navigator.navigate(
                MediaImagePreviewDialogDestination(imagePath = imagePath),
            )
        }
    }

    override fun showLinkLoaderSheet(media: MediaMetadata, episode: Episode?) {
        runOnResumed {
            navigator.navigate(
                MediaLinksBottomSheetDestination(
                    media = media,
                    episode = episode,
                )
            )
        }
    }

    override fun showPlayerSplashScreen(
        media: MediaMetadata,
        episode: Episode?,
        initialStreamUrl: String?,
        initialCacheId: String?,
        initialHeaders: Map<String, String>?
    ) {
        runOnResumed {
            navigator.navigate(
                PlayerSplashScreenDestination(
                    media = media,
                    episode = episode,
                    initialStreamUrl = initialStreamUrl,
                    initialCacheId = initialCacheId,
                    initialHeaders = initialHeaders?.let {
                        PlayerScreenInitialHeader(headers = it)
                    }
                ),
            )
        }
    }

    override fun navigateToPlayerScreen(
        media: MediaMetadata,
        episode: Episode?,
        initialStreamUrl: String?,
        initialCacheId: String?,
        initialHeaders: Map<String, String>?
    ) {
        runOnResumed {
            navigator.navigate(
                PlayerScreenDestination(
                    media = media,
                    episode = episode,
                    initialStreamUrl = initialStreamUrl,
                    initialCacheId = initialCacheId,
                    initialHeaders = initialHeaders?.let {
                        PlayerScreenInitialHeader(headers = it)
                    }
                ),
            ) {
                // Clear player splash screen from back stack to prevent going back to it
                popUpTo(
                    PlayerSplashScreenDestination(
                        media = media,
                        episode = episode,
                        initialStreamUrl = initialStreamUrl,
                        initialCacheId = initialCacheId,
                        initialHeaders = initialHeaders?.let {
                            PlayerScreenInitialHeader(headers = it)
                        }
                    )
                ) {
                    inclusive = true
                }
            }
        }
    }

    override fun navigateToAddProviderScreen(initialSelectedRepositoryFilter: Repository?) {
        runOnResumed {
            navigator.navigate(
                AddProviderScreenDestination(initialSelectedRepositoryFilter = initialSelectedRepositoryFilter),
            )
        }
    }

    override fun navigateToSubSettingsScreen(route: SubSettingsNavItem) {
        runOnResumed {
            when (route) {
                SubSettingsNavItem.APPEARANCE -> {
                    navigator.navigate(AppearanceTweakScreenDestination)
                }
                SubSettingsNavItem.PLAYER -> {
                    navigator.navigate(PlayerTweakScreenDestination)
                }
                SubSettingsNavItem.DATA -> {
                    navigator.navigate(DataTweakScreenDestination)
                }
                SubSettingsNavItem.PROVIDERS -> {
                    navigator.navigate(ProvidersTweakScreenDestination)
                }
                SubSettingsNavItem.SYSTEM -> {
                    navigator.navigate(SystemTweakScreenDestination)
                }
            }
        }
    }

    override fun openSubtitlesSettings() {
        runOnResumed {
            navigator.navigate(SubtitlesTweakScreenDestination)
        }
    }

    override fun navigateToMediaLinkCardsTweakScreen() {
        runOnResumed {
            navigator.navigate(MediaLinkCardsTweakScreenDestination)
        }
    }

    override fun navigateToManageMediaLinksScreen(
        media: MediaMetadata,
        episode: Episode?
    ) {
        runOnResumed {
            navigator.navigate(
                ManageMediaLinksTweakScreenDestination(
                    media = media,
                    episode = episode
                )
            )
        }
    }

    override fun navigateToManageShowLinksScreen(media: MediaMetadata) {
        runOnResumed {
            navigator.navigate(
                MediaLinksShowDetailTweakScreenDestination(
                    media = media
                )
            )
        }
    }
}
