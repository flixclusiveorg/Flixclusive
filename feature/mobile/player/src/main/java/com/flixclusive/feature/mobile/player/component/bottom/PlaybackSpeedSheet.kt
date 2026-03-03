package com.flixclusive.feature.mobile.player.component.bottom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.fadingEdge
import com.flixclusive.core.presentation.common.extensions.noOpClickable
import com.flixclusive.core.presentation.mobile.components.GlassSurface
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.player.AppPlayer
import com.flixclusive.core.presentation.player.ui.state.PlaybackSpeedState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import com.flixclusive.core.presentation.player.R as PlayerR

private val QuickSpeeds by lazy { listOf(0.5f, 1f, 1.5f, 2f, 2.5f) }

private val SpeedRange get() = AppPlayer.playbackSpeedRange

private const val TICK_STEP = 0.1f
private const val TICK_WIDTH_DP = 24f
private const val RULER_HEIGHT_DP = 60

private val Float.toPlayerSpeed: String
    get() = String.format(Locale.ROOT, "%.1fx", this)

private fun Float.toTickIndex(): Int =
    ((this - SpeedRange.start) / TICK_STEP).roundToInt()
        .coerceIn(0, ((SpeedRange.endInclusive - SpeedRange.start) / TICK_STEP).roundToInt())

@Composable
internal fun PlaybackSpeedSheet(
    playbackSpeedState: PlaybackSpeedState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.small
    val scope = rememberCoroutineScope()
    var speed by remember { mutableFloatStateOf(playbackSpeedState.playbackSpeed) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = speed.toTickIndex()
    )

    BackHandler {
        onDismiss()
    }

    GlassSurface(
        shape = shape,
        modifier = modifier
            .noOpClickable()
            .fillMaxAdaptiveWidth(
                compact = 0.5f,
                medium = 0.6f,
                expanded = 0.65f
            ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(PlayerR.string.speed),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White
                ),
            )

            Text(
                text = speed.toPlayerSpeed,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White
                ),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            )

            ScrollableTickRuler(
                listState = listState,
                onValueChange = { speed = it },
                onValueChangeFinished = {
                    playbackSpeedState.updatePlaybackSpeed(speed)
                },
                valueRange = SpeedRange,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                QuickSpeeds.forEach { preset ->
                    val isSelected = abs(speed - preset) < TICK_STEP / 2f

                    TextButton(
                        onClick = {
                            speed = preset
                            playbackSpeedState.updatePlaybackSpeed(preset)
                            scope.launch {
                                listState.animateScrollToItem(preset.toTickIndex())
                            }
                        },
                        shape = shape,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 1.dp, minHeight = 30.dp)
                            .weight(1f),
                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder() else null,
                    ) {
                        Text(
                            text = preset.toPlayerSpeed,
                            style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun ScrollableTickRuler(
    listState: LazyListState,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tickWidthDp = TICK_WIDTH_DP.dp
    val tickWidthPx = with(density) { tickWidthDp.toPx() }
    val totalTicks = ((valueRange.endInclusive - valueRange.start) / TICK_STEP).roundToInt()

    val tickColor = Color.White.copy(alpha = 0.25f)
    val indicatorColor = Color.White

    LaunchedEffect(listState) {
        snapshotFlow {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val raw = index + offset / tickWidthPx
            val tick = raw.roundToInt().coerceIn(0, totalTicks)
            valueRange.start + tick * TICK_STEP
        }
            .distinctUntilChanged()
            .collect { onValueChange(it) }
    }

    LaunchedEffect(listState) {
        var wasScrolling = false
        snapshotFlow { listState.isScrollInProgress }
            .debounce(800)
            .collect { scrolling ->
                if (wasScrolling && !scrolling) onValueChangeFinished()
                wasScrolling = scrolling
            }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val halfWidth = maxWidth / 2
        val tickHalf = tickWidthDp / 2

        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(
                lazyListState = listState,
                snapPosition = SnapPosition.Center,
            ),
            contentPadding = PaddingValues(horizontal = halfWidth - tickHalf),
            modifier = Modifier
                .fillMaxWidth()
                .height(RULER_HEIGHT_DP.dp)
                .fadingEdge(
                    scrollableState = listState,
                    orientation = Orientation.Horizontal,
                    edgeSize = 80.dp,
                ),
        ) {
            items(totalTicks + 1) {
                Canvas(
                    modifier = Modifier
                        .width(tickWidthDp)
                        .height(RULER_HEIGHT_DP.dp)
                ) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val h = size.height * 0.45f

                    drawLine(
                        color = tickColor,
                        start = Offset(centerX, centerY - h / 2f),
                        end = Offset(centerX, centerY + h / 2f),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .width(2.dp)
                .height(RULER_HEIGHT_DP.dp)
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val h = size.height * 0.65f

            drawLine(
                color = indicatorColor,
                start = Offset(centerX, centerY - h / 2f),
                end = Offset(centerX, centerY + h / 2f),
                strokeWidth = size.width,
                cap = StrokeCap.Round,
            )
        }
    }
}
