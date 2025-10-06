package com.flixclusive.mobile

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.navigation.navigator.ExitAction
import com.flixclusive.core.navigation.navigator.StartPlayerAction
import com.flixclusive.core.navigation.navigator.ViewFilmPreviewAction
import com.flixclusive.core.presentation.mobile.components.NetworkMonitorSnackbarVisuals
import com.flixclusive.core.presentation.mobile.components.NetworkMonitorSnackbarVisuals.Companion.NetworkMonitorSnackbarHost
import com.flixclusive.core.presentation.mobile.components.provider.MediaLinksBottomSheet
import com.flixclusive.core.presentation.mobile.util.LocalGlobalScaffoldPadding
import com.flixclusive.core.presentation.mobile.util.PipModeUtil.rememberIsInPipMode
import com.flixclusive.core.util.webview.WebViewDriver
import com.flixclusive.feature.mobile.app.updates.destinations.AppUpdatesScreenDestination
import com.flixclusive.feature.mobile.film.destinations.FilmScreenDestination
import com.flixclusive.feature.mobile.markdown.destinations.MarkdownScreenDestination
import com.flixclusive.feature.mobile.player.destinations.PlayerScreenDestination
import com.flixclusive.feature.mobile.profiles.destinations.UserProfilesScreenDestination
import com.flixclusive.feature.mobile.provider.add.destinations.AddProviderScreenDestination
import com.flixclusive.feature.mobile.searchExpanded.destinations.SearchExpandedScreenDestination
import com.flixclusive.feature.mobile.user.add.destinations.AddUserScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinSetupScreenDestination
import com.flixclusive.feature.mobile.user.destinations.PinVerifyScreenDestination
import com.flixclusive.feature.mobile.user.destinations.UserAvatarSelectScreenDestination
import com.flixclusive.feature.mobile.user.destinations.UserEditScreenDestination
import com.flixclusive.feature.splashScreen.destinations.SplashScreenDestination
import com.flixclusive.mobile.component.BottomBar
import com.flixclusive.mobile.component.DisplayChangelogsObserver
import com.flixclusive.mobile.component.FilmCoverPreview
import com.flixclusive.mobile.component.FilmPreviewBottomSheet
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.navigation.AppNavHost
import com.flixclusive.navigation.MobileNavGraphs
import com.flixclusive.navigation.extensions.bottomBarNavigate
import com.flixclusive.navigation.extensions.currentScreenAsState
import com.flixclusive.navigation.extensions.navigateIfResumed
import com.ramcosta.composedestinations.dynamic.within
import com.ramcosta.composedestinations.spec.Route
import com.ramcosta.composedestinations.utils.currentDestinationFlow
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import com.flixclusive.core.strings.R as LocaleR

@SuppressLint("DiscouragedApi", "UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileActivity.MobileApp(viewModel: MobileAppViewModel) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasNotSeenNewChangelogs by viewModel.hasNotSeenNewChangelogs.collectAsStateWithLifecycle()
    val isConnectedAtNetwork by viewModel.hasInternet.collectAsStateWithLifecycle()

    val isInPipMode = rememberIsInPipMode()
    val webViewDriver by viewModel.webViewDriver.collectAsStateWithLifecycle()

    var hasBeenDisconnected by remember { mutableStateOf(false) }
    var fullScreenImageToShow: String? by remember { mutableStateOf(null) }
    var wasOnPlayerScreen by remember { mutableStateOf(false) } // Track if we were on the player screen

    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val skipPartiallyExpanded by remember { mutableStateOf(true) }
    val bottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
        )

    val navController = rememberNavController()
    val currentSelectedScreen by navController.currentDestinationFlow.collectAsStateWithLifecycle(
        initialValue = MobileNavGraphs.root.startRoute,
    )
    val currentNavGraph by navController.currentScreenAsState(MobileNavGraphs.home)

    fun onStartPlayer(playerData: PlayerData) {
        val (film, episode) = playerData
        navController.navigateIfResumed(PlayerScreenDestination(film = film as FilmMetadata, episode = episode))
        viewModel.updateLoadLinksState(LoadLinksState.Idle)
    }

    DisplayChangelogsObserver(
        navController = navController,
        hasNotSeenNewChangelogs = hasNotSeenNewChangelogs,
        currentSelectedScreen = currentSelectedScreen,
        onSaveLastSeenChangelogs = viewModel::onSaveLastSeenChangelogs,
    )

    LaunchedEffect(uiState.loadLinksState) {
        if (
            uiState.loadLinksState is LoadLinksState.Success &&
            uiState.playerData != null &&
            currentSelectedScreen != PlayerScreenDestination
        ) {
            onStartPlayer(uiState.playerData!!)
        }
    }

    // Detect when we leave the PlayerScreen to stop loading links if needed
    LaunchedEffect(currentSelectedScreen) {
        val isPlayerScreen = currentSelectedScreen == PlayerScreenDestination
        val isGoingBackFromPlayerScreen = wasOnPlayerScreen && !isPlayerScreen

        if (isGoingBackFromPlayerScreen) {
            viewModel.onStopLoadingLinks(isForceClosing = true)
        }

        wasOnPlayerScreen = isPlayerScreen
    }

    LaunchedEffect(isConnectedAtNetwork) {
        if (!isConnectedAtNetwork) {
            hasBeenDisconnected = true
            snackbarHostState.showSnackbar(
                NetworkMonitorSnackbarVisuals(
                    message = UiText.from(LocaleR.string.offline_message).asString(context),
                    isDisconnected = true,
                ),
            )
        } else if (hasBeenDisconnected) {
            hasBeenDisconnected = false
            snackbarHostState.showSnackbar(
                NetworkMonitorSnackbarVisuals(
                    message = UiText.StringResource(LocaleR.string.online_message).asString(context),
                    isDisconnected = false,
                ),
            )
        }
    }

    val useBottomBar =
        remember(currentSelectedScreen) {
            shouldHideBottomBar(route = currentSelectedScreen)
        }

    val windowInsets = when (currentSelectedScreen) {
        SplashScreenDestination -> WindowInsets.systemBars
        else -> WindowInsets(0.dp)
    }

    Scaffold(
        contentWindowInsets = windowInsets,
        snackbarHost = {
            if (!isInPipMode) {
                NetworkMonitorSnackbarHost(hostState = snackbarHostState)
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = useBottomBar,
                enter = slideInVertically(tween(450)) { it },
                exit = slideOutVertically(tween(400)) { it },
            ) {
                BottomBar(
                    currentSelectedScreen = currentNavGraph,
                    onNavigate = {
                        navController.bottomBarNavigate(
                            screen = it,
                            currentNavGraph = currentNavGraph,
                        )
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
                exitAction = remember {
                    object : ExitAction {
                        override fun onExitApplication() {
                            finish()
                            exitProcess(0)
                        }
                    }
                },
                previewFilmAction = remember {
                    object : ViewFilmPreviewAction {
                        override fun previewFilm(film: Film) {
                            viewModel.previewFilm(film)
                        }
                    }
                },
                startPlayerAction = remember {
                    object : StartPlayerAction {
                        override fun play(
                            film: Film,
                            episode: Episode?,
                        ) {
                            viewModel.onFetchMediaLinks(film, episode)
                        }
                    }
                },
            )
        }
    }

    if (currentSelectedScreen == PlayerScreenDestination) {
        if (uiState.filmPreviewState != null) {
            val film = uiState.filmPreviewState!!.film

            val navigateToFilmScreen = {
                navController.navigateIfResumed(
                    direction = FilmScreenDestination(film = film) within currentNavGraph,
                )
                viewModel.onRemovePreviewFilm()
            }

            FilmPreviewBottomSheet(
                preview = uiState.filmPreviewState!!,
                sheetState = bottomSheetState,
                onSeeMoreClick = {
                    navigateToFilmScreen()

                    scope
                        .launch {
                            bottomSheetState.hide()
                        }.invokeOnCompletion {
                            if (!bottomSheetState.isVisible) {
                                viewModel.onRemovePreviewFilm()
                            }
                        }
                },
                onDismissRequest = viewModel::onRemovePreviewFilm,
                onPlayClick = {
                    viewModel.onFetchMediaLinks(film)
                    navigateToFilmScreen()
                },
                onImageClick = {
                    fullScreenImageToShow = it
                },
            )
        }

        if (!uiState.loadLinksState.isIdle) {
            val cachedLinks by viewModel.currentLinksCache.collectAsStateWithLifecycle()

            LaunchedEffect(true) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            MediaLinksBottomSheet(
                state = uiState.loadLinksState,
                streams = cachedLinks?.streams ?: emptyList(),
                subtitles = cachedLinks?.subtitles ?: emptyList(),
                onLinkClick = { /*TODO: Add link chooser navigation for player screen*/ },
                onSkipLoading = {
                    if (uiState.playerData != null) {
                        onStartPlayer(uiState.playerData!!)
                    }
                },
                onDismiss = {
                    viewModel.onStopLoadingLinks(isForceClosing = true)
                    viewModel.onRemovePreviewFilm() // In case, the bottom sheet is opened
                },
            )
        } else {
            LaunchedEffect(true) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        AnimatedVisibility(
            visible = fullScreenImageToShow != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            fullScreenImageToShow?.let {
                FilmCoverPreview(
                    imagePath = it,
                    onDismiss = {
                        fullScreenImageToShow = null
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
}

private fun shouldHideBottomBar(route: Route): Boolean {
    val noBottomBarScreens =
        listOf(
            AddProviderScreenDestination,
            AddUserScreenDestination,
            MarkdownScreenDestination,
            PinSetupScreenDestination,
            PinVerifyScreenDestination,
            PlayerScreenDestination,
            SplashScreenDestination,
            AppUpdatesScreenDestination,
            UserAvatarSelectScreenDestination,
            UserEditScreenDestination,
            UserProfilesScreenDestination,
        )

    val noBottomBarNestedScreens =
        listOf(SearchExpandedScreenDestination.within(MobileNavGraphs.search).route)

    return noBottomBarNestedScreens.none { it == route.route } &&
        noBottomBarScreens.none { it == route }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebViewDriverDialog(
    webView: WebViewDriver,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        properties =
            DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            ),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight(0.9F)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(tonalElevation = 3.dp) {
                Box(
                    modifier =
                        Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .padding(bottom = 6.dp),
                ) {
                    Text(
                        text = webView.name,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        modifier =
                            Modifier
                                .padding(10.dp),
                    )
                }
            }

            AndroidView(
                modifier =
                    Modifier
                        .weight(0.7F)
                        .alpha(0.99F)
                        .fillMaxWidth()
                        .padding(26.dp),
                factory = {
                    webView.apply {
                        layoutParams =
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                    }
                },
            )
        }
    }
}
