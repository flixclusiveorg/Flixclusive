package com.flixclusive.mobile

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.navigation.ModalBottomSheetLayout
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.navigation.rememberBottomSheetNavigator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.navigation.plusAssign
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.navigation.navigator.NavigatorExitApp
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.mobile.components.NetworkMonitorSnackbarVisuals
import com.flixclusive.core.presentation.mobile.components.NetworkMonitorSnackbarVisuals.Companion.NetworkMonitorSnackbarHost
import com.flixclusive.core.presentation.mobile.components.provider.ProviderCrashBottomSheet
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.presentation.mobile.util.PipModeUtil.rememberIsInPipMode
import com.flixclusive.mobile.component.BottomBar
import com.flixclusive.mobile.component.WebViewDriverDialog
import com.flixclusive.navigation.AppNavHost
import com.flixclusive.navigation.extensions.bottomBarNavigate
import com.flixclusive.navigation.extensions.currentScreenAsState
import com.ramcosta.composedestinations.generated.appmobile.AppmobileNavGraphs
import com.ramcosta.composedestinations.generated.appmobile.navgraphs.AppGraph
import com.ramcosta.composedestinations.generated.appupdates.destinations.AppUpdatesScreenDestination
import com.ramcosta.composedestinations.generated.markdown.destinations.MarkdownScreenDestination
import com.ramcosta.composedestinations.generated.onboarding.destinations.OnboardingScreenDestination
import com.ramcosta.composedestinations.generated.player.destinations.PlayerScreenDestination
import com.ramcosta.composedestinations.generated.player.destinations.PlayerSplashScreenDestination
import com.ramcosta.composedestinations.generated.provideradd.destinations.AddProviderScreenDestination
import com.ramcosta.composedestinations.generated.providersettings.destinations.ProviderSettingsScreenDestination
import com.ramcosta.composedestinations.generated.splashscreen.destinations.SplashScreenDestination
import com.ramcosta.composedestinations.generated.useradd.destinations.AddUserScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.PinSetupScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.PinVerifyScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.UserAvatarSelectScreenDestination
import com.ramcosta.composedestinations.generated.useredit.destinations.UserEditScreenDestination
import com.ramcosta.composedestinations.generated.userprofiles.destinations.UserProfilesScreenDestination
import com.ramcosta.composedestinations.spec.Route
import com.ramcosta.composedestinations.utils.currentDestinationFlow
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
import kotlinx.coroutines.FlowPreview
import kotlin.system.exitProcess
import com.flixclusive.core.strings.R as LocaleR

@SuppressLint("DiscouragedApi", "UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
internal fun MobileActivity.MobileApp(viewModel: MobileAppViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
//    val hasNotSeenNewChangelogs by viewModel.hasNotSeenNewChangelogs.collectAsStateWithLifecycle()
    val isConnectedAtNetwork by viewModel.hasInternet.collectAsStateWithLifecycle()

    val isInPipMode = rememberIsInPipMode()
    val webViewDriver by viewModel.webViewDriver.collectAsStateWithLifecycle()

    var hasBeenDisconnected by remember { mutableStateOf(false) }

    val snackBarHostState = remember { SnackbarHostState() }

    val navController = rememberNavController()
    val destinationsNavigator = navController.rememberDestinationsNavigator()
    val currentSelectedScreen by navController.currentDestinationFlow.collectAsStateWithLifecycle(
        initialValue = AppGraph.startRoute
    )
    val currentNavGraph by navController.currentScreenAsState(AppmobileNavGraphs.home)

    val bottomSheetNavigator = rememberBottomSheetNavigator()
    navController.navigatorProvider += bottomSheetNavigator

    var useBottomBar by remember {
        mutableStateOf(shouldHideBottomBar(route = currentSelectedScreen))
    }

    LaunchedEffect(currentSelectedScreen) {
        useBottomBar = shouldHideBottomBar(route = currentSelectedScreen)
    }

    LaunchedEffect(uiState.isLoadingProviders) {
        if (uiState.isLoadingProviders && currentSelectedScreen != SplashScreenDestination) {
            destinationsNavigator.navigate(SplashScreenDestination) {
                popUpTo(AppGraph) {
                    inclusive = true
                }
            }
        }
    }

    LaunchedEffect(isConnectedAtNetwork) {
        if (!isConnectedAtNetwork) {
            hasBeenDisconnected = true
            snackBarHostState.showSnackbar(
                NetworkMonitorSnackbarVisuals(
                    message = UiText.from(LocaleR.string.offline_message).asString(context),
                    isDisconnected = true,
                ),
            )
        } else if (hasBeenDisconnected) {
            hasBeenDisconnected = false
            snackBarHostState.showSnackbar(
                NetworkMonitorSnackbarVisuals(
                    message = UiText.from(LocaleR.string.online_message).asString(context),
                    isDisconnected = false,
                ),
            )
        }
    }

    ModalBottomSheetLayout(
        bottomSheetNavigator = bottomSheetNavigator,
        sheetBackgroundColor = Color.Transparent,
        sheetElevation = 0.dp,
        sheetContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Scaffold(
            contentWindowInsets = when (currentSelectedScreen) {
                SplashScreenDestination -> WindowInsets.systemBars
                else -> WindowInsets()
            },
            snackbarHost = {
                if (!isInPipMode) {
                    NetworkMonitorSnackbarHost(hostState = snackBarHostState)
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = useBottomBar,
                    enter = slideInVertically(tween(450, delayMillis = 300)) { it },
                    exit = slideOutVertically(tween(400)) { it },
                ) {
                    BottomBar(
                        currentSelectedGraph = currentNavGraph,
                        onNavigate = {
                            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                destinationsNavigator.bottomBarNavigate(
                                    screen = it,
                                    currentSelectedScreen = currentSelectedScreen,
                                    currentNavGraph = currentNavGraph,
                                )
                            }
                        },
                    )
                }
            },
        ) { padding ->
            CompositionLocalProvider(
                LocalGlobalScaffoldPadding provides padding,
            ) {
                AppNavHost(
                    navController = navController,
                    navigatorExitApp = remember {
                        object : NavigatorExitApp {
                            override fun exitApplication() {
                                finish()
                                exitProcess(0)
                            }
                        }
                    },
                )
            }
        }
    }

    if (webViewDriver != null) {
        WebViewDriverDialog(
            webView = webViewDriver!!,
            onDismiss = viewModel::hideWebViewDriver,
        )
    }

    if (uiState.providerErrors.isNotEmpty()) {
        val listOfErrors by remember {
            derivedStateOf {
                uiState.providerErrors.values.toList()
            }
        }

        ProviderCrashBottomSheet(
            isLoading = uiState.isLoadingProviders,
            errors = listOfErrors,
            onDismissRequest = {
                if (uiState.isLoadingProviders) {
                    context.showToast(
                        resources.getString(LocaleR.string.sheet_dismiss_disabled_on_provider_loading),
                    )
                    return@ProviderCrashBottomSheet
                }

                viewModel.onConsumeProviderErrors()
            },
        )
    }
}

private fun shouldHideBottomBar(route: Route): Boolean {
    val noBottomBarScreens = listOf(
        AddProviderScreenDestination,
        AddUserScreenDestination,
        MarkdownScreenDestination,
        AppUpdatesScreenDestination,
        OnboardingScreenDestination,
        PinSetupScreenDestination,
        PinVerifyScreenDestination,
        PlayerScreenDestination,
        PlayerSplashScreenDestination,
        ProviderSettingsScreenDestination,
        SplashScreenDestination,
        UserAvatarSelectScreenDestination,
        UserEditScreenDestination,
        UserProfilesScreenDestination,
    )

    return !noBottomBarScreens.fastAny { it == route }
}
