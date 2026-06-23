package com.flixclusive.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.flixclusive.core.navigation.navargs.PinVerificationResult
import com.flixclusive.core.navigation.navargs.PinWithHintResult
import com.flixclusive.core.navigation.navigator.NavigatorExitApp
import com.flixclusive.feature.mobile.library.details.LibraryDetailsScreen
import com.flixclusive.feature.mobile.library.details.LibraryDetailsViewModel
import com.flixclusive.feature.mobile.media.MediaScreen
import com.flixclusive.feature.mobile.media.MediaScreenViewModel
import com.flixclusive.feature.mobile.seeAll.SeeAllScreen
import com.flixclusive.feature.mobile.seeAll.SeeAllViewModel
import com.flixclusive.feature.mobile.user.add.AddUserScreen
import com.flixclusive.feature.mobile.user.profiles.UserProfilesScreen
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.appmobile.AppmobileNavGraphs
import com.ramcosta.composedestinations.generated.appmobile.destinations.HomeCommonMediaScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.HomeCommonSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.LibraryCommonMediaScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.LibraryCommonSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SearchCommonMediaScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SearchCommonSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SettingsCommonMediaScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.destinations.SettingsCommonSeeAllScreenDestination
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.AppGraph
import com.ramcosta.composedestinations.generated.librarydetails.destinations.LibraryDetailsScreenDestination
import com.ramcosta.composedestinations.generated.useradd.destinations.AddUserScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.PinSetupScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.PinVerifyScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.UserAvatarSelectScreenDestination
import com.ramcosta.composedestinations.generated.useredit.navtype.pinVerificationResultNavType
import com.ramcosta.composedestinations.generated.useredit.navtype.pinWithHintResultNavType
import com.ramcosta.composedestinations.generated.userprofiles.destinations.UserProfilesScreenDestination
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navargs.primitives.intNavType
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.navigation.require
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.scope.resultRecipient
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun AppNavHost(
    navController: NavHostController,
    navigatorExitApp: NavigatorExitApp,
) {
    val navigator = navController.rememberDestinationsNavigator()

    DestinationsNavHost(
        engine = rememberNavHostEngine(),
        navController = navController,
        navGraph = AppGraph,
        dependenciesContainerBuilder = {
            dependency(
                getMobileNavigator(
                    navBackStackEntry = navBackStackEntry,
                    navigatorExitApp = navigatorExitApp,
                    navigator = navigator,
                ),
            )
        },
    ) {
        composable(AddUserScreenDestination) {
            val dependencies = buildDependencies()
            AddUserScreen(
                isInitializing = navArgs.isInitializing,
                navigator = dependencies.require(),
                avatarResultRecipient = resultRecipient<UserAvatarSelectScreenDestination, Int>(
                    resultNavType = intNavType,
                ),
                pinResultRecipient = resultRecipient<PinSetupScreenDestination, PinWithHintResult>(
                    resultNavType = pinWithHintResultNavType
                ),
            )
        }

        composable(UserProfilesScreenDestination) {
            val dependencies = buildDependencies()
            UserProfilesScreen(
                isFromSplashScreen = navArgs.isFromSplashScreen,
                navigator = dependencies.require(),
                pinVerifyResultRecipient = resultRecipient<PinVerifyScreenDestination, PinVerificationResult>(
                    resultNavType = pinVerificationResultNavType
                ),
            )
        }

        persistentListOf(
            HomeCommonMediaScreenDestination to AppmobileNavGraphs.home,
            SearchCommonMediaScreenDestination to AppmobileNavGraphs.search,
            LibraryCommonMediaScreenDestination to AppmobileNavGraphs.library,
            SettingsCommonMediaScreenDestination to AppmobileNavGraphs.settings,
        ).fastForEach { (destination, graph) ->
            composable(destination) {
                val dependencies = buildDependencies()
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(graph.route)
                }

                MediaScreen(
                    navArgs = navArgs,
                    navigator = dependencies.require(),
                    viewModel = hiltViewModel<MediaScreenViewModel, MediaScreenViewModel.Factory>(
                        key = navArgs.media.id + navArgs.media.providerId,
                        viewModelStoreOwner = parentEntry,
                        creationCallback = { it.create(navArgs = navArgs.media) }
                    )
                )
            }
        }

        persistentListOf(
            HomeCommonSeeAllScreenDestination to AppmobileNavGraphs.home,
            SearchCommonSeeAllScreenDestination to AppmobileNavGraphs.search,
            LibraryCommonSeeAllScreenDestination to AppmobileNavGraphs.library,
            SettingsCommonSeeAllScreenDestination to AppmobileNavGraphs.settings,
        ).fastForEach { (destination, graph) ->
            composable(destination) {
                val dependencies = buildDependencies()
                val parentEntry = remember(navBackStackEntry) {
                    navController.getBackStackEntry(graph.route)
                }

                SeeAllScreen(
                    navArgs = navArgs,
                    navigator = dependencies.require(),
                    viewModel = hiltViewModel<SeeAllViewModel, SeeAllViewModel.Factory>(
                        key = navArgs.catalog.url + navArgs.catalog.providerId,
                        viewModelStoreOwner = parentEntry,
                        creationCallback = { it.create(navArgs = navArgs.catalog) }
                    )
                )
            }
        }

        composable(LibraryDetailsScreenDestination) {
            val dependencies = buildDependencies()
            val parentEntry = remember(navBackStackEntry) {
                navController.getBackStackEntry(AppmobileNavGraphs.library.route)
            }

            LibraryDetailsScreen(
                navigator = dependencies.require(),
                navArgs = navArgs,
                viewModel = hiltViewModel<LibraryDetailsViewModel, LibraryDetailsViewModel.Factory>(
                    key = navArgs.library.id + navArgs.tracker?.id,
                    viewModelStoreOwner = parentEntry,
                    creationCallback = { it.create(navArgs = navArgs) }
                )
            )
        }
    }
}

@Composable
private fun getMobileNavigator(
    navBackStackEntry: NavBackStackEntry,
    navigatorExitApp: NavigatorExitApp,
    navigator: DestinationsNavigator,
): MobileAppNavigator {
    return MobileAppNavigator(
        destination = navBackStackEntry.destination,
        navigator = navigator,
        uriHandler = LocalUriHandler.current,
        lifecycleOwner = LocalLifecycleOwner.current,
        navigatorExitApp = navigatorExitApp,
    )
}
