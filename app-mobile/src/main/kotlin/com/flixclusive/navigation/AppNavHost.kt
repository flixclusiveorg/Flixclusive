package com.flixclusive.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.flixclusive.core.navigation.navargs.PinVerificationResult
import com.flixclusive.core.navigation.navargs.PinWithHintResult
import com.flixclusive.core.navigation.navigator.ExitAction
import com.flixclusive.core.navigation.navigator.StartPlayerAction
import com.flixclusive.core.navigation.navigator.ViewFilmPreviewAction
import com.flixclusive.feature.mobile.profiles.UserProfilesScreen
import com.flixclusive.feature.mobile.profiles.destinations.UserProfilesScreenDestination
import com.flixclusive.feature.mobile.user.add.AddUserScreen
import com.flixclusive.feature.mobile.user.add.destinations.AddUserScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinSetupScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinVerifyScreenDestination
import com.flixclusive.feature.mobile.user.destinations.UserAvatarSelectScreenDestination
import com.flixclusive.navigation.extensions.defaultEnterTransition
import com.flixclusive.navigation.extensions.defaultExitTransition
import com.flixclusive.navigation.extensions.defaultPopEnterTransition
import com.flixclusive.navigation.extensions.defaultPopExitTransition
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.scope.resultRecipient

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@Composable
internal fun AppNavHost(
    navController: NavHostController,
    exitAction: ExitAction,
    previewFilmAction: ViewFilmPreviewAction,
    startPlayerAction: StartPlayerAction,
    isTv: Boolean = false,
) {
    DestinationsNavHost(
        engine = rememberAnimatedNavHostEngine(
            rootDefaultAnimations = RootNavGraphDefaultAnimations(
                enterTransition = { defaultEnterTransition(isTv, initialState, targetState) },
                exitTransition = { defaultExitTransition(isTv, initialState, targetState) },
                popEnterTransition = { defaultPopEnterTransition(isTv) },
                popExitTransition = { defaultPopExitTransition(isTv) },
            ),
        ),
        navController = navController,
        navGraph = MobileNavGraphs.root,
        dependenciesContainerBuilder = {
            dependency(
                getMobileNavigator(
                    navBackStackEntry = navBackStackEntry,
                    exitAction = exitAction,
                    navController = navController,
                    previewFilmAction = previewFilmAction,
                    startPlayerAction = startPlayerAction,
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
                        exitAction = exitAction,
                        navController = navController,
                        previewFilmAction = previewFilmAction,
                        startPlayerAction = startPlayerAction,
                    ),
                    avatarResultRecipient = resultRecipient<UserAvatarSelectScreenDestination, Int>(),
                    pinResultRecipient = resultRecipient<PinSetupScreenDestination, PinWithHintResult>(),
                )
            }

            composable(UserProfilesScreenDestination) {
                UserProfilesScreen(
                    isFromSplashScreen = navArgs.isFromSplashScreen,
                    navigator = getMobileNavigator(
                        navBackStackEntry = navBackStackEntry,
                        exitAction = exitAction,
                        navController = navController,
                        previewFilmAction = previewFilmAction,
                        startPlayerAction = startPlayerAction,
                    ),
                    pinVerifyResultRecipient = resultRecipient<PinVerifyScreenDestination, PinVerificationResult>(),
                )
            }
        }
    }
}

@Composable
private fun getMobileNavigator(
    navBackStackEntry: NavBackStackEntry,
    exitAction: ExitAction,
    navController: NavHostController,
    previewFilmAction: ViewFilmPreviewAction,
    startPlayerAction: StartPlayerAction,
): MobileAppNavigator {
    return MobileAppNavigator(
        destination = navBackStackEntry.destination,
        navController = navController,
        uriHandler = LocalUriHandler.current,
        exitAction = exitAction,
        previewFilmAction = previewFilmAction,
        startPlayerAction = startPlayerAction,
    )
}
