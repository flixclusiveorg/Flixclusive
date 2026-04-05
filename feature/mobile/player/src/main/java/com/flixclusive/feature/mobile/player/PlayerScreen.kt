package com.flixclusive.feature.mobile.player

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
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
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.presentation.common.extensions.getActivity
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.mobile.util.PipModeUtil.rememberIsInPipMode
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.ui.ComposePlayer
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState.Companion.rememberPlayerSnackbarState
import com.flixclusive.core.util.log.warnLog
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.player.component.PlayerControls
import com.flixclusive.feature.mobile.player.component.effect.ToggleOrientationEffect
import com.flixclusive.feature.mobile.player.component.effect.ToggleSystemBarsEffect
import com.flixclusive.feature.mobile.player.component.server.ProviderLoadingDialog
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
    val failedStreamUrls by viewModel.failedStreamUrls.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canSkipLoading by viewModel.canSkipLoading.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val currentProvider = remember(uiState.currentProvider, providers) {
        providers.find { it.id == uiState.currentProvider }
    }

    val snackbarState = rememberPlayerSnackbarState()

    fun showErrorAndGoBack() {
        context.showToast(resources.getString(R.string.no_servers_error))
        navigator.goBack()

        val activity = context.getActivity<ComponentActivity>()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    LaunchedEffect(Unit) {
        if (servers.isEmpty()) {
            showErrorAndGoBack()
            return@LaunchedEffect
        }

        viewModel.player.errors.collect { error ->
            snackbarState.showError(error.asString(context))
        }
    }

    if (currentProvider == null) {
        if (providers.isNotEmpty()) {
            LaunchedEffect(Unit) {
                warnLog("Current provider with id ${uiState.currentProvider} not found in providers list")
                showErrorAndGoBack()
            }
        }
        return
    }

    PlayerScreenContent(
        player = viewModel.player,
        film = args.film,
        playerPreferences = playerPreferences,
        subtitlesPreferences = subtitlesPreferences,
        snackbarState = snackbarState,
        currentEpisode = currentEpisode,
        currentProvider = currentProvider,
        providers = providers,
        servers = { servers },
        failedStreamUrls = { failedStreamUrls },
        currentSeason = { currentSeason },
        currentServer = { uiState.currentServer },
        loadLinksState = { uiState.loadLinksState },
        canSkipLoading = { canSkipLoading },
        onEpisodeChange = viewModel::onEpisodeChange,
        onServerChange = viewModel::onServerChange,
        onProviderChange = { viewModel.onProviderChange(it.id) },
        onSkipProviderLoading = viewModel::onSkipProviderLoading,
        onCancelLoading = viewModel::onCancelLoading,
        onServerFail = viewModel::onServerFail,
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
    failedStreamUrls: () -> Set<String>,
    currentSeason: () -> SeasonWithProgress?,
    currentServer: () -> Int,
    currentProvider: ProviderMetadata,
    providers: List<ProviderMetadata>,
    loadLinksState: () -> LoadLinksState,
    canSkipLoading: () -> Boolean,
    snackbarState: PlayerSnackbarState,
    onBack: () -> Unit,
    onServerChange: (Int) -> Unit,
    onServerFail: (Int) -> Unit,
    onProviderChange: (ProviderMetadata) -> Unit,
    onSkipProviderLoading: () -> Unit,
    onCancelLoading: () -> Unit,
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
    ToggleOrientationEffect()

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
            failedStreamUrls = failedStreamUrls,
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
            onServerFail = onServerFail,
            onResizeModeChange = { resizeMode = it },
        )

        val state = loadLinksState()
        if (state.isLoading || state.isError) {
            ProviderLoadingDialog(
                state = state,
                canSkipLoading = canSkipLoading(),
                onSkipLoading = onSkipProviderLoading,
                onDismiss = onCancelLoading,
            )
        }
    }
}
