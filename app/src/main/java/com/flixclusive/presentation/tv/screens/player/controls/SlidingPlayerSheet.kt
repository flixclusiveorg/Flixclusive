package com.flixclusive.presentation.tv.screens.player.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.flixclusive.R
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.NonFocusableSpacer
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.colorOnMediumEmphasisTv
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.focusOnInitialVisibility
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.handleDPadKeyEvents
import com.flixclusive.presentation.utils.ComposeUtils.applyDropShadow
import com.flixclusive.presentation.utils.ModifierUtils.fadingEdge
import com.flixclusive.presentation.utils.ModifierUtils.ifElse
import com.flixclusive.providers.models.common.Subtitle
import com.flixclusive.providers.models.common.VideoDataServer

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SlidingPlayerSheet(
    modifier: Modifier = Modifier,
    sideSheetFocusPriority: BottomControlsButtonType?,
    servers: List<VideoDataServer>,
    subtitles: List<Subtitle>,
    qualities: List<String>,
    selectedSubtitle: Int,
    selectedQuality: Int,
    selectedServer: Int,
    onSubtitleChange: (Int) -> Unit,
    onQualityChange: (Int) -> Unit,
    onDismissSheet: () -> Unit,
) {
    val headerStyles = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.SemiBold,
        color = Color.White
    ).applyDropShadow()

    val topFade = Brush.verticalGradient(
        0F to Color.Transparent,
        0.1F to Color.Red
    )
    val bottomFade = Brush.verticalGradient(
        0.8f to Color.Red,
        0.9f to Color.Transparent
    )

    val serversListState = rememberTvLazyListState()
    val qualitiesListState = rememberTvLazyListState()
    val subtitlesListState = rememberTvLazyListState()

    val shouldFocusOnQualities = remember { mutableStateOf(false) }
    val shouldFocusOnSubtitles = remember { mutableStateOf(false) }
    val shouldFocusOnServers = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        qualitiesListState.scrollToItem(selectedQuality)
        subtitlesListState.scrollToItem(selectedSubtitle)
        serversListState.scrollToItem(selectedServer)
    }

    TvLazyColumn(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        modifier = modifier
            .focusGroup()
            .fillMaxWidth(0.38F)
            .fillMaxHeight()
            .drawBehind {
                drawRect(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black
                        ),
                        startX = 0F,
                        endX = size.width.times(0.7F)
                    )
                )
            }
    ) {
        item {
            NonFocusableSpacer(height = 15.dp)
        }

        if (servers.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.server) + " - " + servers[selectedServer],
                    style = headerStyles,
                    modifier = Modifier.padding(start = 16.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                TvLazyColumn(
                    modifier = Modifier
                        .height(100.dp)
                        .fadingEdge(topFade)
                        .fadingEdge(bottomFade),
                    state = serversListState,
                    pivotOffsets = PivotOffsets(0.2F)
                ) {
                    item {
                        NonFocusableSpacer(height = 15.dp)
                    }

                    itemsIndexed(servers) { i, server ->
                        SlidingSheetItem(
                            modifier = Modifier
                                .handleDPadKeyEvents(onLeft = onDismissSheet)
                                .focusProperties {
                                    if (i == 0)
                                        up = FocusRequester.Cancel
                                }
                                .ifElse(
                                    condition = sideSheetFocusPriority == BottomControlsButtonType.Server && i == selectedServer,
                                    ifTrueModifier = Modifier.focusOnInitialVisibility(
                                        shouldFocusOnServers
                                    )
                                ),
                            name = server.serverName,
                            index = i,
                            selectedIndex = selectedServer,
                            onClick = {
                                onQualityChange(i)
                                onDismissSheet()
                            }
                        )
                    }

                    item {
                        NonFocusableSpacer(height = 35.dp)
                    }
                }
            }
        }

        if (qualities.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.quality) + " - " + qualities[selectedQuality],
                    style = headerStyles,
                    modifier = Modifier.padding(start = 16.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                TvLazyColumn(
                    modifier = Modifier
                        .height(150.dp)
                        .fadingEdge(topFade)
                        .fadingEdge(bottomFade),
                    state = qualitiesListState,
                    pivotOffsets = PivotOffsets(0.2F)
                ) {
                    item {
                        NonFocusableSpacer(height = 15.dp)
                    }

                    itemsIndexed(qualities) { i, quality ->
                        SlidingSheetItem(
                            modifier = Modifier
                                .handleDPadKeyEvents(onLeft = onDismissSheet)
                                .ifElse(
                                    condition = sideSheetFocusPriority == BottomControlsButtonType.Quality && i == selectedQuality,
                                    ifTrueModifier = Modifier.focusOnInitialVisibility(
                                        shouldFocusOnQualities
                                    )
                                ),
                            name = quality,
                            index = i,
                            selectedIndex = selectedQuality,
                            onClick = {
                                onQualityChange(i)
                                onDismissSheet()
                            }
                        )
                    }

                    item {
                        NonFocusableSpacer(height = 35.dp)
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.subtitle) + " - " + subtitles[selectedSubtitle].lang,
                style = headerStyles,
                modifier = Modifier.padding(start = 16.dp),
                fontWeight = FontWeight.Bold
            )
        }

        item {
            TvLazyColumn(
                modifier = Modifier
                    .height(250.dp)
                    .fadingEdge(topFade)
                    .fadingEdge(bottomFade),
                state = subtitlesListState,
                pivotOffsets = PivotOffsets(0.4F)
            ) {
                item {
                    NonFocusableSpacer(height = 20.dp)
                }

                itemsIndexed(subtitles) { i, subtitle ->
                    SlidingSheetItem(
                        modifier = Modifier
                            .handleDPadKeyEvents(onLeft = onDismissSheet)
                            .focusProperties {
                                if (i == subtitles.lastIndex)
                                    down = FocusRequester.Cancel
                            }
                            .ifElse(
                                condition = sideSheetFocusPriority == BottomControlsButtonType.Subtitle && i == selectedSubtitle,
                                ifTrueModifier = Modifier.focusOnInitialVisibility(
                                    shouldFocusOnSubtitles
                                )
                            ),
                        name = subtitle.lang,
                        index = i,
                        selectedIndex = selectedSubtitle,
                        onClick = {
                            onSubtitleChange(i)
                            onDismissSheet()
                        }
                    )
                }

                item(6) {
                    NonFocusableSpacer(height = 20.dp)
                }
            }
        }

        item {
            NonFocusableSpacer(height = 15.dp)
        }
    }
}

@Composable
private fun SlidingSheetItem(
    modifier: Modifier = Modifier,
    name: String,
    index: Int,
    selectedIndex: Int,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val baseStyle = MaterialTheme.typography.bodyMedium

    val style = remember(selectedIndex) {
        if (selectedIndex == index)
            baseStyle.copy(fontWeight = FontWeight.SemiBold)
        else baseStyle.copy(fontWeight = FontWeight.Medium)
    }

    val textPadding by animateDpAsState(
        targetValue = if (isFocused) 16.dp else 8.dp,
        label = ""
    )

    Surface(
        modifier = modifier
            .onFocusChanged {
                isFocused = it.isFocused
            },
        border = ClickableSurfaceDefaults.border(
            border = Border.None,
            focusedBorder = Border.None
        ),
        shape = ClickableSurfaceDefaults.shape(RectangleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = colorOnMediumEmphasisTv(),
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White
        ),
        onClick = onClick
    ) {
        Text(
            text = name,
            style = style.applyDropShadow(),
            modifier = Modifier.padding(
                start = 16.dp,
                top = textPadding,
                end = textPadding,
                bottom = textPadding,
            )
        )
    }
}