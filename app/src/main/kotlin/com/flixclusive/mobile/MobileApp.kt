package com.flixclusive.mobile

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.flixclusive.core.ui.mobile.InternetMonitorSnackbar
import com.flixclusive.core.ui.mobile.InternetMonitorSnackbarVisuals
import com.flixclusive.core.ui.mobile.component.provider.ProviderResourceStateDialog
import com.flixclusive.core.util.common.ui.UiText
import com.flixclusive.feature.mobile.film.destinations.FilmScreenDestination
import com.flixclusive.feature.mobile.player.destinations.PlayerScreenDestination
import com.flixclusive.feature.mobile.searchExpanded.destinations.SearchExpandedScreenDestination
import com.flixclusive.feature.mobile.update.destinations.UpdateScreenDestination
import com.flixclusive.feature.splashScreen.destinations.SplashScreenDestination
import com.flixclusive.mobile.component.BottomBar
import com.flixclusive.mobile.component.FilmCoverPreview
import com.flixclusive.mobile.component.FilmPreviewBottomSheet
import com.flixclusive.model.provider.MediaLinkResourceState
import com.flixclusive.util.AppNavHost
import com.flixclusive.util.currentScreenAsState
import com.flixclusive.util.navigateIfResumed
import com.ramcosta.composedestinations.dynamic.within
import com.ramcosta.composedestinations.navigation.navigate
import com.ramcosta.composedestinations.utils.currentDestinationFlow
import com.ramcosta.composedestinations.utils.startDestination
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import com.flixclusive.core.util.R as UtilR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileActivity.MobileApp(
    viewModel: MobileAppViewModel
) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isConnectedAtNetwork by viewModel.isConnectedAtNetwork.collectAsStateWithLifecycle()

    val filmToPreview by viewModel.filmToPreview.collectAsStateWithLifecycle()
    val episodeToPlay by viewModel.episodeToPlay.collectAsStateWithLifecycle()

    var hasBeenDisconnected by remember { mutableStateOf(false) }
    var fullScreenImageToShow: String? by remember { mutableStateOf(null) }

    val scope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    val skipPartiallyExpanded by remember { mutableStateOf(true) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )

    val navController = rememberNavController()
    val currentSelectedScreen by navController.currentDestinationFlow.collectAsStateWithLifecycle(initialValue = MobileNavGraphs.root.startRoute)
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
                    message = UiText.StringResource(UtilR.string.offline_message).asString(context),
                    isDisconnected = true
                )
            )
        } else if (hasBeenDisconnected) {
            hasBeenDisconnected = false
            snackbarHostState.showSnackbar(
                InternetMonitorSnackbarVisuals(
                    message = UiText.StringResource(UtilR.string.online_message).asString(context),
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

    if(!uiState.isOnPlayerScreen) {
        if(uiState.isShowingBottomSheetCard && filmToPreview != null) {
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
                        if(!bottomSheetState.isVisible) {
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

        if (uiState.mediaLinkResourceState !is MediaLinkResourceState.Idle) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            Box(
                contentAlignment = Alignment.Center
            ) {
                ProviderResourceStateDialog(
                    state = uiState.mediaLinkResourceState,
                    canSkipExtractingPhase = cachedLinks?.streams?.isNotEmpty() == true,
                    onSkipExtractingPhase = onStartPlayer,
                    onConsumeDialog = {
                        viewModel.onConsumeSourceDataDialog(isForceClosing = true)
                        viewModel.onBottomSheetClose() // In case, the bottom sheet is opened
                    }
                )
            }
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
}