package com.flixclusive.mobile

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.common.provider.MediaLinkResourceState
import com.flixclusive.core.ui.mobile.InternetMonitorSnackbar
import com.flixclusive.core.ui.mobile.InternetMonitorSnackbarVisuals
import com.flixclusive.core.ui.mobile.component.provider.MediaLinksBottomSheet
import com.flixclusive.core.util.webview.WebViewDriver
import com.flixclusive.feature.mobile.film.destinations.FilmScreenDestination
import com.flixclusive.feature.mobile.markdown.destinations.MarkdownScreenDestination
import com.flixclusive.feature.mobile.player.destinations.PlayerScreenDestination
import com.flixclusive.feature.mobile.searchExpanded.destinations.SearchExpandedScreenDestination
import com.flixclusive.feature.mobile.update.destinations.UpdateScreenDestination
import com.flixclusive.feature.splashScreen.destinations.SplashScreenDestination
import com.flixclusive.mobile.component.BottomBar
import com.flixclusive.mobile.component.FilmCoverPreview
import com.flixclusive.mobile.component.FilmPreviewBottomSheet
import com.flixclusive.util.AppNavHost
import com.flixclusive.util.currentScreenAsState
import com.flixclusive.util.navigateIfResumed
import com.ramcosta.composedestinations.dynamic.within
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.utils.currentDestinationFlow
import com.ramcosta.composedestinations.utils.startDestination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import com.flixclusive.core.locale.R as LocaleR

@SuppressLint("DiscouragedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileActivity.MobileApp(
    viewModel: MobileAppViewModel
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hasSeenChangelogsForCurrentBuild by viewModel.hasSeenChangelogsForCurrentBuild.collectAsStateWithLifecycle()
    val isConnectedAtNetwork by viewModel.isConnectedAtNetwork.collectAsStateWithLifecycle()

    val filmToPreview by viewModel.filmToPreview.collectAsStateWithLifecycle()
    val episodeToPlay by viewModel.episodeToPlay.collectAsStateWithLifecycle()

    val webViewDriver by viewModel.webViewDriver.collectAsStateWithLifecycle()

    var hasBeenDisconnected by remember { mutableStateOf(false) }
    var fullScreenImageToShow: String? by remember { mutableStateOf(null) }

    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val skipPartiallyExpanded by remember { mutableStateOf(true) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )

    val navController = rememberNavController()
    val currentSelectedScreen by navController.currentDestinationFlow.collectAsStateWithLifecycle(
        initialValue = MobileNavGraphs.root.startRoute
    )
    val currentNavGraph by navController.currentScreenAsState(MobileNavGraphs.home)

    val cachedLinks = viewModel.loadedCachedLinks

    val onStartPlayer = {
        viewModel.setPlayerModeState(isInPlayer = true)
        viewModel.onConsumeSourceDataDialog()

        navController.navigateIfResumed(
            PlayerScreenDestination(
                film = filmToPreview!!,
                episodeToPlay = episodeToPlay
            )
        )
    }

    LaunchedEffect(hasSeenChangelogsForCurrentBuild, currentSelectedScreen) {
        if (hasSeenChangelogsForCurrentBuild && currentSelectedScreen != SplashScreenDestination) {
            delay(1000L) // Add delay for smooth transition
            val changelogsId = context.resources
                .getIdentifier(
                    /* name = */ "changelog_${viewModel.currentVersionCode}",
                    /* defType = */ "array",
                    /* defPackage = */ context.packageName
                )

            if (changelogsId == 0)
                return@LaunchedEffect

            val (title, changelogs) = context.resources.getStringArray(changelogsId)

            navController.navigateIfResumed(
                direction = MarkdownScreenDestination(
                    title = title,
                    description = changelogs
                )
            ) {
                launchSingleTop = true
                restoreState = true
            }

            viewModel.onSaveLastSeenChangelogsVersion(
                version = viewModel.currentVersionCode
            )
        }
    }

    LaunchedEffect(cachedLinks?.streams?.size, uiState.mediaLinkResourceState) {
        if (
            uiState.mediaLinkResourceState == MediaLinkResourceState.Success
            && currentSelectedScreen != PlayerScreenDestination
        ) {
            onStartPlayer()
        }
    }

    LaunchedEffect(currentSelectedScreen) {
        val isPlayerScreen = currentSelectedScreen == PlayerScreenDestination
        val isGoingBackFromPlayerScreen = uiState.isOnPlayerScreen && !isPlayerScreen

        if (isGoingBackFromPlayerScreen) {
            viewModel.onConsumeSourceDataDialog(isForceClosing = true)
        }

        viewModel.setPlayerModeState(isInPlayer = isPlayerScreen)
    }

    LaunchedEffect(isConnectedAtNetwork) {
        if (!isConnectedAtNetwork) {
            hasBeenDisconnected = true
            snackbarHostState.showSnackbar(
                InternetMonitorSnackbarVisuals(
                    message = UiText.StringResource(LocaleR.string.offline_message).asString(context),
                    isDisconnected = true
                )
            )
        } else if (hasBeenDisconnected) {
            hasBeenDisconnected = false
            snackbarHostState.showSnackbar(
                InternetMonitorSnackbarVisuals(
                    message = UiText.StringResource(LocaleR.string.online_message).asString(context),
                    isDisconnected = false
                )
            )
        }
    }

    val useBottomBar = remember(currentSelectedScreen) {
        currentSelectedScreen.route != SearchExpandedScreenDestination.within(MobileNavGraphs.search).route
            && currentSelectedScreen != PlayerScreenDestination
            && currentSelectedScreen != SplashScreenDestination
            && currentSelectedScreen != UpdateScreenDestination
            && currentSelectedScreen != MarkdownScreenDestination
    }

    val windowInsets = when (currentSelectedScreen) {
        SplashScreenDestination -> WindowInsets.systemBars
        else -> WindowInsets(0.dp)
    }

    Scaffold(
        contentWindowInsets = windowInsets,
        snackbarHost = {
            if (!viewModel.isInPipMode) {
                InternetMonitorSnackbar(hostState = snackbarHostState)
            }
        },
        bottomBar = {
            if (useBottomBar) {
                BottomBar(
                    currentSelectedScreen = currentNavGraph,
                    onNavigate = { screen ->
                        navController.run {
                            val isPoppingToRoot = screen == currentNavGraph

                            navigate(screen) {
                                if (isPoppingToRoot) {
                                    popUpTo(screen.startRoute.route)
                                } else {
                                    popUpTo(MobileNavGraphs.home.startDestination.route) {
                                        saveState = true
                                    }
                                }

                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            AppNavHost(
                navController = navController,
                previewFilm = viewModel::previewFilm,
                play = { film, episode ->
                    viewModel.onPlayClick(
                        film = film,
                        episode = episode
                    )
                },
                closeApp = {
                    finish()
                    exitProcess(0)
                }
            )
        }
    }

    if (!uiState.isOnPlayerScreen) {
        if (uiState.isShowingBottomSheetCard && filmToPreview != null) {
            val navigateToFilmScreen = {
                navController.navigateIfResumed(
                    direction = FilmScreenDestination(
                        film = filmToPreview!!,
                        startPlayerAutomatically = false
                    ) within currentNavGraph
                )
                viewModel.onBottomSheetClose()
            }

            FilmPreviewBottomSheet(
                film = filmToPreview!!,
                isInWatchlist = { uiState.isLongClickedFilmInWatchlist },
                isInWatchHistory = { uiState.isLongClickedFilmInWatchHistory },
                sheetState = bottomSheetState,
                onWatchlistButtonClick = viewModel::onWatchlistButtonClick,
                onWatchHistoryButtonClick = viewModel::onRemoveButtonClick,
                onSeeMoreClick = {
                    navigateToFilmScreen()

                    scope.launch {
                        bottomSheetState.hide()
                    }.invokeOnCompletion {
                        if (!bottomSheetState.isVisible) {
                            viewModel.onBottomSheetClose()
                        }
                    }
                },
                onDismissRequest = viewModel::onBottomSheetClose,
                onPlayClick = {
                    viewModel.onPlayClick()
                    navigateToFilmScreen()
                },
                onImageClick = {
                    fullScreenImageToShow = it
                }
            )
        }

        if (!uiState.mediaLinkResourceState.isIdle) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            MediaLinksBottomSheet(
                state = uiState.mediaLinkResourceState,
                streams = cachedLinks?.streams ?: emptyList(),
                subtitles = cachedLinks?.subtitles ?: emptyList(),
                onLinkClick = {},
                onSkipLoading = onStartPlayer,
                onDismiss = {
                    viewModel.onConsumeSourceDataDialog(isForceClosing = true)
                    viewModel.onBottomSheetClose() // In case, the bottom sheet is opened
                }
            )
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        AnimatedVisibility(
            visible = fullScreenImageToShow != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            fullScreenImageToShow?.let {
                FilmCoverPreview(
                    imagePath = it,
                    onDismiss = {
                        fullScreenImageToShow = null
                    }
                )
            }
        }
    }

    if (webViewDriver != null) {
        WebViewDriverDialog(
            webView = webViewDriver!!,
            onDismiss = viewModel::hideWebViewDriver
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebViewDriverDialog(
    webView: WebViewDriver,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9F)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(tonalElevation = 3.dp) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        text = webView.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .padding(10.dp)
                    )
                }
            }

            AndroidView(
                modifier = Modifier
                    .weight(0.7F)
                    .alpha(0.99F)
                    .fillMaxWidth()
                    .padding(26.dp),
                factory = {
                    webView.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                }
            )
        }
    }
}