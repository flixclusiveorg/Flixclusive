package com.flixclusive.feature.mobile.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.common.extensions.getActivity
import com.flixclusive.core.presentation.mobile.util.PipModeUtil.rememberIsInPipMode
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.ui.ComposePlayer
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.player.component.PlayerControls
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph

@Destination<ExternalModuleGraph>(
    navArgs = PlayerScreenNavArgs::class,
)
@Composable
internal fun PlayerScreen(
    navigator: PlayerScreenNavigator,
    args: PlayerScreenNavArgs,
    viewModel: PlayerScreenViewModel = hiltViewModel(),
) {
    val playerPreferences by viewModel.playerPreferences.collectAsStateWithLifecycle()
    val subtitlesPreferences by viewModel.subtitlesPreferences.collectAsStateWithLifecycle()

    val currentEpisode by viewModel.selectedEpisode.collectAsStateWithLifecycle()
    val currentSeason by viewModel.seasonToDisplay.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentProvider = remember(uiState.selectedProvider) {
        viewModel.providers.find {
            it.id == uiState.selectedProvider
        } ?: throw IllegalStateException("Selected provider not found in the list of providers")
    }

    PlayerScreenContent(
        player = viewModel.player,
        film = args.film,
        playerPreferences = playerPreferences,
        subtitlesPreferences = subtitlesPreferences,
        currentEpisode = currentEpisode,
        currentSeason = currentSeason,
        currentProvider = currentProvider,
        providers = viewModel.providers,
        onBack = navigator::goBack,
        onEpisodeChange = viewModel::onEpisodeChange,
        onProviderChange = {
            viewModel.onProviderChange(it.id)
        },
        onSeasonChange = {
            viewModel.onSeasonChange(it.number)
        },
        // TODO: Handle next episode/season/provider logic in the viewmodel instead of the UI layer
        onNext = null,
    )
}

@Composable
internal fun PlayerScreenContent(
    player: AppPlayer,
    film: FilmMetadata,
    playerPreferences: PlayerPreferences,
    subtitlesPreferences: SubtitlesPreferences,
    currentEpisode: Episode?,
    currentSeason: SeasonWithProgress?,
    currentProvider: ProviderMetadata,
    providers: List<ProviderMetadata>,
    onBack: () -> Unit,
    onProviderChange: (ProviderMetadata) -> Unit,
    onEpisodeChange: (Episode) -> Unit,
    onSeasonChange: (Season) -> Unit,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.getActivity<Activity>()
    val isInPipMode = rememberIsInPipMode()
    var resizeMode by rememberSaveable { mutableStateOf(playerPreferences.resizeMode) }

    BackHandler(onBack = onBack)

    DisposableEffect(LocalLifecycleOwner.current) {
        context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        context.toggleSystemBars(isVisible = false)

        onDispose {
            // TODO: Watch out orientation changes when user selects a different episode/season/provider,
            //  maybe we should only reset orientation when user leaves the player screen
            context.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            context.toggleSystemBars(isVisible = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        ComposePlayer(
            player = player,
            isInPipMode = isInPipMode,
            resizeMode = resizeMode,
        )

        PlayerControls(
            player = player,
            film = film,
            isInPipMode = isInPipMode,
            playerPrefs = playerPreferences,
            subtitlesPrefs = subtitlesPreferences,
            currentEpisode = currentEpisode,
            currentSeason = currentSeason,
            currentResizeMode = resizeMode,
            onEpisodeChange = currentEpisode?.let { onEpisodeChange },
            onSeasonChange = currentSeason?.let { onSeasonChange },
            onNext = onNext,
            onBack = onBack,
            currentProvider = currentProvider,
            providers = providers,
            onProviderChange = onProviderChange,
            onResizeModeChange = { resizeMode = it },
        )
    }
}

@Suppress("DEPRECATION")
private fun Activity.toggleSystemBars(isVisible: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (isVisible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.ime())
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        return
    }

    val state = if(!isVisible) {
        (window.decorView.systemUiVisibility
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN)
    } else (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

    if (window.decorView.systemUiVisibility != state) {
        window.decorView.systemUiVisibility = state
    }
}
