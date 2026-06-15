package com.flixclusive.feature.mobile.player

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.util.fastFirstOrNull
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.navigation.navigator.NavigateBack
import com.flixclusive.core.presentation.common.extensions.getActivity
import com.flixclusive.core.presentation.common.extensions.showToast
import com.flixclusive.core.presentation.mobile.extensions.toggleSystemBars
import com.flixclusive.core.presentation.mobile.util.PipModeUtil.rememberIsInPipMode
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.model.track.PlayerServer
import com.flixclusive.core.presentation.player.ui.ComposePlayer
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState
import com.flixclusive.core.presentation.player.ui.state.PlayerSnackbarState.Companion.rememberPlayerSnackbarState
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.feature.mobile.player.component.PlayerControls
import com.flixclusive.feature.mobile.player.component.effect.ToggleOrientationEffect
import com.flixclusive.feature.mobile.player.component.effect.ToggleSystemBarsEffect
import com.flixclusive.feature.mobile.player.component.server.ProviderLoadingDialog
import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season
import com.flixclusive.model.provider.ProviderMetadata
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.ExternalModuleGraph
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge

@OptIn(FlowPreview::class)
@Destination<ExternalModuleGraph>(
    navArgs = PlayerScreenNavArgs::class,
)
@Composable
internal fun PlayerScreen(
    navigator: NavigateBack,
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
    val canSkipLoading by viewModel.canSkipLoading.collectAsStateWithLifecycle()
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val currentProvider by remember {
        derivedStateOf {
            val providers = (providers as? Async.Success)?.data ?: emptyList()
            providers.fastFirstOrNull { it.id == uiState.currentProvider }
        }
    }

    val snackbarState = rememberPlayerSnackbarState()

    fun showErrorAndGoBack(message: String) {
        context.showToast(message)
        navigator.navigateBack()

        val activity = context.getActivity<ComponentActivity>()
        activity.toggleSystemBars(true)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    LaunchedEffect(viewModel) {
        var areServersLoaded = false

        viewModel.servers
            .filterIsInstance<Async.Success<List<PlayerServer>>>()
            .mapLatest { it.data }
            .distinctUntilChanged()
            .collectLatest {
                if (it.isEmpty() && !areServersLoaded) {
                    showErrorAndGoBack(resources.getString(R.string.error_no_servers_go_back))
                    return@collectLatest
                } else if (it.isEmpty()) {
                    snackbarState.showError(resources.getString(R.string.error_no_servers))
                    return@collectLatest
                }

                areServersLoaded = true
            }
    }

    LaunchedEffect(viewModel) {
        val providersFlow = viewModel.providers
            .filterIsInstance<Async.Success<List<ProviderMetadata>>>()
            .mapLatest { it.data }
            .distinctUntilChanged()

        val currentProviderFlow = viewModel.uiState
            .mapLatest { it.currentProvider }
            .distinctUntilChanged()

        combine(
            providersFlow,
            currentProviderFlow
        ) { providers, currentProviderId ->
            providers to currentProviderId
        }.collectLatest { (providers, currentProviderId) ->
            val currentProvider = providers.fastFirstOrNull { it.id == currentProviderId }
            if (providers.isNotEmpty() || currentProvider != null) {
                cancel()
                return@collectLatest
            }

            showErrorAndGoBack(resources.getString(R.string.error_no_providers))
        }
    }

    LaunchedEffect(viewModel) {
        merge(
            viewModel.player.errors,
            viewModel.playerErrors
        ).collect { error ->
            snackbarState.showError(error.asString(context))
        }
    }

    if (currentProvider == null) {
        BackHandler {
            navigator.navigateBack()

            val activity = context.getActivity<ComponentActivity>()
            activity.toggleSystemBars(true)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    PlayerScreenContent(
        player = viewModel.player,
        media = args.media,
        playerPreferences = playerPreferences,
        subtitlesPreferences = subtitlesPreferences,
        snackbarState = snackbarState,
        currentEpisode = currentEpisode,
        currentProvider = { currentProvider!! },
        providers = { (providers as? Async.Success)?.data ?: emptyList() },
        servers = { (servers as? Async.Success)?.data ?: emptyList() },
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
            navigator.navigateBack()
        },
    )
}

@Composable
internal fun PlayerScreenContent(
    player: AppPlayer,
    media: MediaMetadata,
    playerPreferences: PlayerPreferences,
    subtitlesPreferences: SubtitlesPreferences,
    currentEpisode: Episode?,
    servers: () -> List<PlayerServer>,
    currentSeason: () -> SeasonWithProgress?,
    currentServer: () -> Int,
    currentProvider: () -> ProviderMetadata,
    providers: () -> List<ProviderMetadata>,
    loadLinksState: () -> LoadLinksState,
    canSkipLoading: () -> Boolean,
    snackbarState: PlayerSnackbarState,
    onBack: () -> Unit,
    onServerChange: (Int) -> Unit,
    onServerFail: (String) -> Unit,
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
            media = media,
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
