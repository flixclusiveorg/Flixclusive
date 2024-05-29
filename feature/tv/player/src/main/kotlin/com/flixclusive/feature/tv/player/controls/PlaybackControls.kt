package com.flixclusive.feature.tv.player.controls

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.GradientCircularProgressIndicator
import com.flixclusive.core.ui.player.FlixclusivePlayerManager
import com.flixclusive.core.ui.player.PlayerUiState
import com.flixclusive.core.ui.player.util.PlayerCacheManager
import com.flixclusive.core.ui.player.util.PlayerUiUtil
import com.flixclusive.core.ui.player.util.PlayerUiUtil.formatMinSec
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.core.util.film.FilmType
import com.flixclusive.feature.tv.player.controls.settings.AudioAndSubtitlesPanel
import com.flixclusive.feature.tv.player.controls.settings.PlaybackSpeedPanel
import com.flixclusive.feature.tv.player.controls.settings.ServersPanel
import com.flixclusive.feature.tv.player.controls.settings.SubtitleStylePanel
import com.flixclusive.feature.tv.player.controls.settings.SubtitleSyncPanel
import com.flixclusive.model.datastore.AppSettings
import com.flixclusive.model.provider.SourceDataState
import com.flixclusive.model.provider.SourceLink
import com.flixclusive.model.provider.Subtitle
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.provider.ProviderApi
import com.flixclusive.provider.dto.FilmInfo
import com.flixclusive.provider.dto.SearchResults
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import kotlin.math.abs
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.util.R as UtilR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun PlaybackControls(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
    currentEpisodeSelected: TMDBEpisode?,
    isSubtitleStylePanelOpened: MutableState<Boolean>,
    isSyncSubtitlesPanelOpened: MutableState<Boolean>,
    isAudioAndSubtitlesPanelOpened: MutableState<Boolean>,
    isPlaybackSpeedPanelOpened: MutableState<Boolean>,
    isServerPanelOpened: MutableState<Boolean>,
    isVisible: Boolean,
    isTvShow: Boolean,
    providerApis: List<ProviderApi>,
    servers: List<SourceLink>,
    stateProvider: () -> PlayerUiState,
    dialogStateProvider: () -> SourceDataState,
    playbackTitle: String,
    isLastEpisode: Boolean,
    seekMultiplier: Long,
    showControls: (Boolean) -> Unit,
    onSeekMultiplierChange: (Long) -> Unit,
    updateAppSettings: (AppSettings) -> Unit,
    onProviderChange: (String) -> Unit,
    onServerChange: (Int) -> Unit,
    onBack: () -> Unit,
    onNextEpisode: () -> Unit,
) {
    val player by rememberLocalPlayerManager()

    val state by rememberUpdatedState(stateProvider())
    val dialogState by rememberUpdatedState(dialogStateProvider())

    val isLoading = remember(player.playbackState, dialogState, seekMultiplier) {
        player.playbackState == Player.STATE_BUFFERING
        && seekMultiplier == 0L
        || dialogState !is SourceDataState.Success
    }

    val topFadeEdge = Brush.verticalGradient(
        0F to Color.Black,
        0.9F to Color.Transparent
    )
    val bottomFadeEdge = Brush.verticalGradient(
        0F to Color.Transparent,
        0.9F to Color.Black
    )

    val isInHours = remember(player.duration) {
        player.duration.formatMinSec().count { it == ':' } == 2
    }
    val isSeeking = remember(seekMultiplier) { seekMultiplier != 0L }
    var seekPosition by remember { mutableLongStateOf(player.currentPosition) }

    val seekingTimeInReverse = remember(seekMultiplier) {
        val timeToSeekTo = appSettings.preferredSeekAmount * seekMultiplier

        "-" + abs((player.duration - (seekPosition + timeToSeekTo))).formatMinSec(isInHours)
    }
    val seekingTime = remember(seekMultiplier) {
        val timeToSeekTo = appSettings.preferredSeekAmount * seekMultiplier

        (seekPosition + timeToSeekTo).formatMinSec(isInHours)
    }

    val noPanelsAreOpen = remember(
        isSubtitleStylePanelOpened.value,
        isSyncSubtitlesPanelOpened.value,
        isAudioAndSubtitlesPanelOpened.value,
        isServerPanelOpened.value,
        isPlaybackSpeedPanelOpened.value,
    ) {
        !isSubtitleStylePanelOpened.value
            && !isSyncSubtitlesPanelOpened.value
            && !isAudioAndSubtitlesPanelOpened.value
            && !isServerPanelOpened.value
            && !isPlaybackSpeedPanelOpened.value
    }

    val areControlsVisible = remember(isVisible, noPanelsAreOpen, isSeeking) {
        isVisible && noPanelsAreOpen && !isSeeking
    }

    val (topSlideEnter, topSlideExit) = slideTransition()
    val (bottomSlideEnter, bottomSlideExit) = slideTransition(
        initialOffsetY = { it },
        targetOffsetY = { it }
    )
    val (bottomHalfSlideEnter, bottomHalfSlideExit) = slideTransition(
        initialOffsetY = { it / 2 },
        targetOffsetY = { it / 2 }
    )

    LaunchedEffect(isSeeking) {
        seekPosition = player.currentPosition
    }

    LaunchedEffect(seekMultiplier) {
        var shouldPlayAfterSeek = false

        showControls(true)
        if(player.isPlaying) {
            player.pause()
            shouldPlayAfterSeek = true
        }

        if (seekMultiplier != 0L) {
            delay(2500)
            val timeToSeekTo = appSettings.preferredSeekAmount * seekMultiplier
            player.seekTo(seekPosition + timeToSeekTo)

            onSeekMultiplierChange(0)
        }

        if(shouldPlayAfterSeek)
            player.play()
    }

    BackHandler(enabled = !areControlsVisible) {
        if (isSeeking) {
            onSeekMultiplierChange(0)
            return@BackHandler
        }

        showControls(true)
    }

    Box(
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = topSlideEnter,
            exit = topSlideExit,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopControls(
                modifier = Modifier.drawBehind {
                    drawRect(topFadeEdge)
                },
                isTvShow = isTvShow,
                isLastEpisode = isLastEpisode,
                title = playbackTitle,
                currentEpisodeSelected = currentEpisodeSelected,
                showControls = { showControls(true) },
                onNavigationIconClick = onBack,
                onNextEpisodeClick = onNextEpisode,
                onServersPanelOpen = { isServerPanelOpened.value = true }
            )
        }

        AnimatedVisibility(
            visible = isSeeking && noPanelsAreOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = if (appSettings.isPlayerTimeReversed) seekingTimeInReverse else seekingTime,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 65.sp
                ),
                color = Color.White
            )
        }

        AnimatedVisibility(
            visible = isLoading && noPanelsAreOpen,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GradientCircularProgressIndicator(
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                )
            )
        }

        AnimatedVisibility(
            visible = areControlsVisible || isSeeking,
            enter = bottomSlideEnter,
            exit = bottomSlideExit,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BottomControls(
                modifier = Modifier.drawBehind {
                    drawRect(brush = bottomFadeEdge)
                },
                isPlayerTimeReversed = appSettings.isPlayerTimeReversed,
                isSeeking = isSeeking,
                onSeekMultiplierChange = onSeekMultiplierChange,
                showControls = { showControls(true) },
                onSubtitleStylePanelOpen = { isSubtitleStylePanelOpened.value = true },
                onSubtitlesPanelOpen = { isAudioAndSubtitlesPanelOpened.value = true },
                onSyncSubtitlesPanelOpen = { isSyncSubtitlesPanelOpened.value = true },
                onSpeedometerPanelOpen = { isPlaybackSpeedPanelOpened.value = true },
            )
        }

        AnimatedVisibility(
            visible =  isSubtitleStylePanelOpened.value,
            enter = bottomHalfSlideEnter,
            exit = bottomHalfSlideExit,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SubtitleStylePanel(
                appSettings = appSettings,
                updateAppSettings = updateAppSettings,
                hidePanel = { isSubtitleStylePanelOpened.value = false },
            )
        }

        AnimatedVisibility(
            visible =  isSyncSubtitlesPanelOpened.value,
            enter = bottomHalfSlideEnter,
            exit = bottomHalfSlideExit,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SubtitleSyncPanel(
                hidePanel = { isSyncSubtitlesPanelOpened.value = false },
            )
        }

        AnimatedVisibility(
            visible =  isAudioAndSubtitlesPanelOpened.value,
            enter = fadeIn() + slideInHorizontally { it / 2 },
            exit = fadeOut() + slideOutHorizontally { it / 2 },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            AudioAndSubtitlesPanel(
                hidePanel = { isAudioAndSubtitlesPanelOpened.value = false },
            )
        }

        AnimatedVisibility(
            visible =  isServerPanelOpened.value,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            ServersPanel(
                state = state,
                servers = servers,
                providerApis = providerApis,
                onProviderChange = onProviderChange,
                onServerChange = onServerChange,
                hidePanel = { isServerPanelOpened.value = false },
            )
        }

        AnimatedVisibility(
            visible =  isPlaybackSpeedPanelOpened.value,
            enter = bottomHalfSlideEnter,
            exit = bottomHalfSlideExit,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            PlaybackSpeedPanel(
                hidePanel = { isPlaybackSpeedPanelOpened.value = false }
            )
        }
    }
}

@Composable
private fun slideTransition(
    initialOffsetY: (fullHeight: Int) -> Int = { -it },
    targetOffsetY: (fullHeight: Int) -> Int = { -it },
): Pair<EnterTransition, ExitTransition> {
    return Pair(
        fadeIn() + slideInVertically(initialOffsetY = initialOffsetY),
        slideOutVertically(targetOffsetY = targetOffsetY) + fadeOut()
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(device = "id:tv_1080p")
@Composable
private fun PlaybackControlsPreview() {
    var seekMultiplier by remember { mutableLongStateOf(0L) }

    val sources = List(5) {
        object : ProviderApi(OkHttpClient()) {
            override val name: String
                get() = "Provider #$it"

            override suspend fun getFilmInfo(filmId: String, filmType: FilmType): FilmInfo {
                TODO("Not yet implemented")
            }

            override suspend fun search(film: Film, page: Int): SearchResults {
                TODO("Not yet implemented")
            }

            override suspend fun getSourceLinks(
                filmId: String,
                film: Film,
                season: Int?,
                episode: Int?,
                onLinkLoaded: (SourceLink) -> Unit,
                onSubtitleLoaded: (Subtitle) -> Unit
            ) {
                TODO("Not yet implemented")
            }
        }
    }

    val serverNames = listOf("ServerA", "ServerB", "ServerC", "ServerD", "ServerE")
    val serverUrls = listOf("http://serverA.com", "http://serverB.com", "http://serverC.com", "http://serverD.com", "http://serverE.com")
    val servers = List(10) {
        SourceLink(
            name = serverNames.random(),
            url = serverUrls.random()
        )
    }

    FlixclusiveTheme(isTv = true) {
        Surface(
            colors = NonInteractiveSurfaceDefaults.colors(Color.Black),
            modifier = Modifier
                .fillMaxSize()
        ) {
            CompositionLocalProvider(
                PlayerUiUtil.LocalPlayerManager provides
                        FlixclusivePlayerManager(
                            OkHttpClient(),
                            LocalContext.current,
                            PlayerCacheManager(LocalContext.current),
                            AppSettings()
                        )
            ) {
                Box {
                    Image(
                        painter = painterResource(id = UiCommonR.drawable.sample_movie_subtitle_preview),
                        contentDescription = stringResource(UtilR.string.sample_movie_content_desc),
                        modifier = Modifier
                            .fillMaxSize()
                    )

                    PlaybackControls(
                        isVisible = true,
                        isTvShow = true,
                        isSubtitleStylePanelOpened = remember { mutableStateOf(false) },
                        isSyncSubtitlesPanelOpened = remember { mutableStateOf(false) },
                        isAudioAndSubtitlesPanelOpened = remember { mutableStateOf(false) },
                        isServerPanelOpened = remember { mutableStateOf(false) },
                        isPlaybackSpeedPanelOpened = remember { mutableStateOf(false) },
                        appSettings = AppSettings(isPlayerTimeReversed = false),
                        stateProvider = { PlayerUiState() },
                        dialogStateProvider = { SourceDataState.Success },
                        playbackTitle = "American Bad Boy",
                        isLastEpisode = false,
                        seekMultiplier = seekMultiplier,
                        showControls = {},
                        onSeekMultiplierChange = {
                            if (it == 0L) {
                                seekMultiplier = 0L
                                return@PlaybackControls
                            }

                            seekMultiplier += it
                        },
                        onBack = { },
                        onNextEpisode = {},
                        updateAppSettings = {},
//                        currentEpisodeSelected = TMDBEpisode(episode = 1, season = 1, title = "American Bad Boy"),
                        currentEpisodeSelected = null,
                        servers = servers,
                        providerApis = sources,
                        onProviderChange = {},
                        onServerChange = {},
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}