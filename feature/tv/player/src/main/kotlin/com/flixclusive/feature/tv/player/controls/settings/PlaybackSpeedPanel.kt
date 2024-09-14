package com.flixclusive.feature.tv.player.controls.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NonInteractiveSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.player.FlixclusivePlayerManager
import com.flixclusive.core.ui.player.util.PlayerCacheManager
import com.flixclusive.core.ui.player.util.PlayerUiUtil
import com.flixclusive.core.ui.player.util.PlayerUiUtil.availablePlaybackSpeeds
import com.flixclusive.core.ui.player.util.PlayerUiUtil.rememberLocalPlayerManager
import com.flixclusive.core.ui.tv.component.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.focusOnInitialVisibility
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.feature.tv.player.controls.settings.common.ConfirmButton
import com.flixclusive.model.datastore.AppSettings
import okhttp3.OkHttpClient
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun PlaybackSpeedPanel(
    hidePanel: () -> Unit,
) {
    val player by rememberLocalPlayerManager()
    val listState = rememberTvLazyListState()

    val sideFade = Brush.horizontalGradient(
        0F to Color.Transparent,
        0.5F to Color.Red,
        1F to Color.Transparent,
    )

    LaunchedEffect(Unit) {
        safeCall {
            listState.scrollToItem(
                index = 3,
                scrollOffset = -250
            )
        }
    }

    BackHandler {
        hidePanel()
    }

    Column(
        modifier = Modifier
            .focusGroup()
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7F)),
        verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(LocaleR.string.current_playback_speed_format, player.playbackSpeed),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black
            ),
            color = LocalContentColor.current.onMediumEmphasis(0.5F),
            modifier = Modifier
                .padding(top = 25.dp)
        )

        TvLazyRow(
            modifier = Modifier
                .fadingEdge(sideFade),
            pivotOffsets = PivotOffsets(0.5F, 0.5F),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            state = listState,
        ) {
            item {
                NonFocusableSpacer(width = 360.dp)
            }

            itemsIndexed(availablePlaybackSpeeds) { i, speed ->
                Surface(
                    onClick = { player.onPlaybackSpeedChange(speed) },
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = LocalContentColor.current.onMediumEmphasis(),
                        focusedContainerColor = Color.Transparent,
                        focusedContentColor = Color.White
                    ),
                    scale = ClickableSurfaceDefaults.scale(
                        focusedScale = 1.3F,
                        pressedScale = 1.2F
                    ),
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                ) {
                    Text(
                        text = "${speed}x",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 65.sp
                        )
                    )
                }
            }

            item {
                NonFocusableSpacer(width = 360.dp)
            }
        }

        ConfirmButton(
            label = stringResource(id = LocaleR.string.close_label),
            isEmphasis = false,
            onClick = hidePanel,
            modifier = Modifier.focusOnInitialVisibility()
        )

    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Preview(device = "id:tv_1080p")
@Composable
private fun PlaybackSpeedPanelPreview() {
    FlixclusiveTheme(isTv = true) {
        Surface(
            colors = NonInteractiveSurfaceDefaults.colors(Color.Black),
            modifier = Modifier
                .fillMaxSize()
        ) {
            CompositionLocalProvider(
                PlayerUiUtil.LocalPlayerManager provides
                        FlixclusivePlayerManager(
                            client = OkHttpClient(),
                            appSettings = AppSettings(),
                            context = LocalContext.current,
                            playerCacheManager = PlayerCacheManager(LocalContext.current),
                            showErrorCallback = {}
                        )
            ) {
                Box {
                    Image(
                        painter = painterResource(id = UiCommonR.drawable.sample_movie_subtitle_preview),
                        contentDescription = stringResource(LocaleR.string.sample_movie_content_desc),
                        modifier = Modifier
                            .fillMaxSize()
                    )

                    PlaybackSpeedPanel {

                    }
                }
            }

        }
    }
}