package com.flixclusive.feature.mobile.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.mobile.util.PipModeUtil.rememberIsInPipMode
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.ui.ComposePlayer
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState.Companion.rememberPlayerSnackbarState
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.player.component.PlayerControls
import com.flixclusive.feature.mobile.player.component.effect.ToggleSystemBarsEffect
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
    val context = LocalContext.current
    val resources = LocalResources.current
    val playerPreferences by viewModel.playerPreferences.collectAsStateWithLifecycle()
    val subtitlesPreferences by viewModel.subtitlesPreferences.collectAsStateWithLifecycle()

    val currentEpisode by viewModel.selectedEpisode.collectAsStateWithLifecycle()
    val currentSeason by viewModel.seasonToDisplay.collectAsStateWithLifecycle()

    val servers by viewModel.servers.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentProvider = remember(uiState.currentProvider) {
        viewModel.providers.find {
            it.id == uiState.currentProvider
        } ?: throw IllegalStateException("Selected provider not found in the list of providers")
    }

    val snackbarState = rememberPlayerSnackbarState()

    LaunchedEffect(Unit) {
        if (servers.isEmpty()) {
            context.showToast(resources.getString(R.string.no_servers_error))
            navigator.goBack()
            return@LaunchedEffect
        }

        viewModel.player.errors.collect { error ->
            snackbarState.showError(error.asString(context))
        }
    }

    PlayerScreenContent(
        player = viewModel.player,
        film = args.film,
        playerPreferences = playerPreferences,
        subtitlesPreferences = subtitlesPreferences,
        snackbarState = snackbarState,
        currentEpisode = currentEpisode,
        currentProvider = currentProvider,
        providers = viewModel.providers,
        servers = { servers },
        currentSeason = { currentSeason },
        currentServer = { uiState.currentServer },
        onEpisodeChange = viewModel::onEpisodeChange,
        onServerChange = viewModel::onServerChange,
        onProviderChange = { viewModel.onProviderChange(it.id) },
        onSeasonChange = { viewModel.onSeasonChange(it.number) },
        onNext = uiState.nextEpisode?.let { { viewModel.onEpisodeChange(episode = it) } },
        onUpdateWatchProgress = {
            if (!viewModel.player.isPlaying && viewModel.player.duration > 0) {
                viewModel.updateWatchProgress()
            }
        },
        onBack = {
            viewModel.updateWatchProgress()
            navigator.goBack()
        },
    )
}

@Composable
internal fun PlayerScreenContent(
    player: AppPlayer,
    film: FilmMetadata,
    playerPreferences: PlayerPreferences,
    subtitlesPreferences: SubtitlesPreferences,
    currentEpisode: Episode?,
    servers: () -> List<PlayerServer>,
    currentSeason: () -> SeasonWithProgress?,
    currentServer: () -> Int,
    currentProvider: ProviderMetadata,
    providers: List<ProviderMetadata>,
    snackbarState: PlayerSnackbarState,
    onBack: () -> Unit,
    onServerChange: (Int) -> Unit,
    onProviderChange: (ProviderMetadata) -> Unit,
    onEpisodeChange: (Episode) -> Unit,
    onSeasonChange: (Season) -> Unit,
    onUpdateWatchProgress: () -> Unit,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isInPipMode = rememberIsInPipMode()
    var resizeMode by remember { mutableStateOf(playerPreferences.resizeMode) }

    BackHandler(onBack = onBack)

    ToggleSystemBarsEffect()

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
            snackbarState = snackbarState,
            isInPipMode = isInPipMode,
            playerPrefs = playerPreferences,
            subtitlesPrefs = subtitlesPreferences,
            currentEpisode = currentEpisode,
            currentSeason = currentSeason,
            currentResizeMode = resizeMode,
            servers = servers,
            currentServer = currentServer,
            onEpisodeChange = currentEpisode?.let { onEpisodeChange },
            onSeasonChange = currentEpisode?.let { onSeasonChange },
            onNext = onNext,
            onBack = onBack,
            currentProvider = currentProvider,
            providers = providers,
            onUpdateWatchProgress = onUpdateWatchProgress,
            onProviderChange = onProviderChange,
            onServerChange = onServerChange,
            onResizeModeChange = { resizeMode = it },
        )
    }
}
