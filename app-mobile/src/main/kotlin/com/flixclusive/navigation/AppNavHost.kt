package com.flixclusive.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.flixclusive.core.navigation.navargs.PinVerificationResult
import com.flixclusive.core.navigation.navargs.PinWithHintResult
import com.flixclusive.core.navigation.navigator.NavigateToLinkLoaderSheet
import com.flixclusive.core.navigation.navigator.NavigatorExitApp
import com.flixclusive.feature.mobile.profiles.UserProfilesScreen
import com.flixclusive.feature.mobile.user.add.AddUserScreen
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.AppGraph
import com.ramcosta.composedestinations.generated.profiles.destinations.UserProfilesScreenDestination
import com.ramcosta.composedestinations.generated.useradd.destinations.AddUserScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.PinSetupScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.PinVerifyScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.UserAvatarSelectScreenDestination
import com.ramcosta.composedestinations.generated.useredit.navtype.pinVerificationResultNavType
import com.ramcosta.composedestinations.generated.useredit.navtype.pinWithHintResultNavType
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navargs.primitives.intNavType
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.scope.resultRecipient
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun AppNavHost(
    navController: NavHostController,
    navigatorExitApp: NavigatorExitApp,
    navigateToLinkLoaderSheet: NavigateToLinkLoaderSheet,
    isTv: Boolean = false,
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
                    navigateToLinkLoaderSheet = navigateToLinkLoaderSheet,
                ),
            )
        },
    ) {
        if (!isTv) {
            composable(AddUserScreenDestination) {
                AddUserScreen(
                    isInitializing = navArgs.isInitializing,
                    navigator = getMobileNavigator(
                        navBackStackEntry = navBackStackEntry,
                        navigatorExitApp = navigatorExitApp,
                        navigator = navigator,
                        navigateToLinkLoaderSheet = navigateToLinkLoaderSheet,
                    ),
                    avatarResultRecipient = resultRecipient<UserAvatarSelectScreenDestination, Int>(
                        resultNavType = intNavType,
                    ),
                    pinResultRecipient = resultRecipient<PinSetupScreenDestination, PinWithHintResult>(
                        resultNavType = pinWithHintResultNavType
                    ),
                )
            }

            composable(UserProfilesScreenDestination) {
                UserProfilesScreen(
                    isFromSplashScreen = navArgs.isFromSplashScreen,
                    navigator = getMobileNavigator(
                        navBackStackEntry = navBackStackEntry,
                        navigatorExitApp = navigatorExitApp,
                        navigator = navigator,
                        navigateToLinkLoaderSheet = navigateToLinkLoaderSheet,
                    ),
                    pinVerifyResultRecipient = resultRecipient<PinVerifyScreenDestination, PinVerificationResult>(
                        resultNavType = pinVerificationResultNavType
                    ),
                )
            }
        }
    }
}

@Composable
private fun getMobileNavigator(
    navBackStackEntry: NavBackStackEntry,
    navigatorExitApp: NavigatorExitApp,
    navigator: DestinationsNavigator,
    navigateToLinkLoaderSheet: NavigateToLinkLoaderSheet,
): MobileAppNavigator {
    return MobileAppNavigator(
        destination = navBackStackEntry.destination,
        navigator = navigator,
        uriHandler = LocalUriHandler.current,
        lifecycleOwner = LocalLifecycleOwner.current,
        navigatorExitApp = navigatorExitApp,
        navigateToLinkLoaderSheet = navigateToLinkLoaderSheet,
    )
}
