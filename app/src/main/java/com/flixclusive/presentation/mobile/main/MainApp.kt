package com.flixclusive.presentation.mobile.main


import android.annotation.SuppressLint
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.flixclusive.R
import com.flixclusive.domain.model.VideoDataDialogState
import com.flixclusive.domain.model.entities.toWatchHistoryItem
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.presentation.NavGraphs
import com.flixclusive.presentation.appCurrentDestinationAsState
import com.flixclusive.presentation.destinations.Destination
import com.flixclusive.presentation.destinations.HomeMobileScreenDestination
import com.flixclusive.presentation.mobile.common.composables.film.FilmBottomSheetPreview
import com.flixclusive.presentation.mobile.common.composables.film.VideoPlayerDialog
import com.flixclusive.presentation.mobile.screens.player.PlayerActivity.Companion.startPlayer
import com.flixclusive.presentation.startAppDestination
import com.flixclusive.presentation.utils.ComposeUtils.navigateSingleTopTo
import com.flixclusive.common.UiText
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import kotlinx.coroutines.launch

@UnstableApi
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialNavigationApi::class,
    ExperimentalAnimationApi::class
)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainActivity.MainApp() {
    val context = LocalContext.current
    val viewModel: MainMobileSharedViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isConnectedAtNetwork by viewModel.isConnectedAtNetwork.collectAsStateWithLifecycle(
        initialValue = true
    )
    val longClickedFilmWatchHistoryItem by viewModel.longClickedFilmWatchHistoryItem.collectAsStateWithLifecycle()
    val videoData by viewModel.videoData.collectAsStateWithLifecycle()

    var hasBeenDisconnected by remember { mutableStateOf(false) }
    var fullScreenImageToShow: String? by remember { mutableStateOf(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val skipPartiallyExpanded by remember { mutableStateOf(true) }
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded
    )

    val engine = rememberAnimatedNavHostEngine()
    val navController = engine.rememberNavController()
    val currentScreen: Destination = navController.appCurrentDestinationAsState().value
        ?: NavGraphs.mobileRoot.startAppDestination

    LaunchedEffect(videoData) {
        if (videoData != null && uiState.videoDataDialogState is VideoDataDialogState.Success) {
            val film = uiState.longClickedFilm
            var seasonCount: Int? = null
            if (film is TvShow) {
                seasonCount = film.seasons.size
            }

            context.startPlayer(
                videoData = videoData,
                watchHistoryItem = longClickedFilmWatchHistoryItem ?: film?.toWatchHistoryItem(),
                seasonCount = seasonCount,
                episodeSelected = uiState.episodeToPlay,
            )

            viewModel.onConsumePlayerDialog()
        }
    }

    LaunchedEffect(isConnectedAtNetwork) {
        if (!isConnectedAtNetwork) {
            hasBeenDisconnected = true
            snackbarHostState.showSnackbar(
                NetworkConnectivitySnackbarVisuals(
                    message = UiText.StringResource(R.string.offline_message).asString(context),
                    isDisconnected = true
                )
            )
        } else if (hasBeenDisconnected) {
            hasBeenDisconnected = false
            snackbarHostState.showSnackbar(
                NetworkConnectivitySnackbarVisuals(
                    message = UiText.StringResource(R.string.online_message).asString(context),
                    isDisconnected = false
                )
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { NetworkConnectivitySnackbar(hostState = snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = uiState.isShowingBottomNavigationBar,
                enter =  fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                MainNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = {
                        navController.navigateSingleTopTo(it, NavGraphs.mobileRoot)
                    },
                    onButtonClickTwice = viewModel::onNavBarItemClickTwice
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            DestinationsNavHost(
                engine = engine,
                navController = navController,
                navGraph = NavGraphs.mobileRoot,
                startRoute = HomeMobileScreenDestination,
                dependenciesContainerBuilder = {
                    dependency(viewModel)
                }
            )
        }
    }

    if (uiState.isShowingBottomSheetCard) {
        FilmBottomSheetPreview(
            film = uiState.longClickedFilm!!,
            isInWatchlist = { uiState.isLongClickedFilmInWatchlist },
            isInWatchHistory = { uiState.isLongClickedFilmInWatchHistory },
            sheetState = bottomSheetState,
            onWatchlistButtonClick = viewModel::onWatchlistButtonClick,
            onWatchHistoryButtonClick = viewModel::onRemoveButtonClick,
            onSeeMoreClick = {
                scope
                    .launch {
                        bottomSheetState.hide()
                    }
                    .invokeOnCompletion {
                        if (!bottomSheetState.isVisible) {
                            viewModel.onSeeMoreClick(shouldSeeMore = true)
                        }
                    }
            },
            onDismissRequest = viewModel::onBottomSheetClose,
            onPlayClick = viewModel::onPlayClick,
            onImageClick = {
                fullScreenImageToShow = it
            }
        )
    }

    if (uiState.videoDataDialogState !is VideoDataDialogState.Idle) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        VideoPlayerDialog(
            videoDataDialogState = uiState.videoDataDialogState,
            onConsumeDialog = {
                viewModel.onConsumePlayerDialog()
                viewModel.onBottomSheetClose() // In case, the bottom sheet is opened
            }
        )
    } else {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fullScreenImageToShow?.let { imagePath ->
        FilmCoverPreview(
            imagePath = imagePath,
            onDismiss = {
                fullScreenImageToShow = null
            }
        )
    }
}