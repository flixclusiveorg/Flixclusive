package com.flixclusive.feature.mobile.player.component.subtitles

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.extensions.dropShadow
import com.flixclusive.core.presentation.common.extensions.fadingEdge
import com.flixclusive.core.presentation.common.extensions.noIndicationClickable
import com.flixclusive.core.presentation.common.extensions.noOpClickable
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.player.model.CueWithTiming
import kotlinx.coroutines.flow.distinctUntilChanged
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.player.R as PlayerR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun SubtitleSyncScreen(
    cues: List<CueWithTiming>,
    currentOffset: Long,
    currentPosition: Long,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tempOffset by remember { mutableLongStateOf(currentOffset) }

    BackHandler {
        onDismiss()
    }

    val activeIndex by remember(cues, currentPosition, currentOffset) {
        derivedStateOf {
            cues.indexOfLast { cue ->
                val adjustedStart = cue.startTimeMs + currentOffset
                currentPosition >= adjustedStart
            }.coerceAtLeast(0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.9F))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .noOpClickable()
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 5.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    AdaptiveIcon(
                        painter = painterResource(id = UiCommonR.drawable.left_arrow),
                        contentDescription = stringResource(id = LocaleR.string.navigate_up),
                        tint = Color.White
                    )
                }

                Spacer(Modifier.weight(1f))

                IconButton(onClick = onDismiss) {
                    AdaptiveIcon(
                        painter = painterResource(id = UiCommonR.drawable.round_close_24),
                        contentDescription = stringResource(id = LocaleR.string.close),
                        tint = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight(0.85F)
            ) {
                SubtitleCuesList(
                    cues = cues,
                    activeIndex = activeIndex,
                    onCueClick = { index ->
                        val cue = cues[index]
                        tempOffset = currentPosition - cue.startTimeMs
                    },
                    modifier = Modifier.weight(1F)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(horizontal = 10.dp)
                        .fillMaxHeight(0.9F)
                        .width(0.5.dp)
                        .background(LocalContentColor.current.copy(alpha = 0.4F))
                )

                OffsetControlPanel(
                    currentOffset = currentOffset,
                    onOffsetChange = { tempOffset = it },
                    onSave = {
                        onSave(tempOffset)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1F)
                )
            }
        }
    }
}

@Composable
private fun SubtitleCuesList(
    cues: List<CueWithTiming>,
    activeIndex: Int,
    onCueClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var userScrolledManually by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrolling ->
                if (isScrolling) {
                    userScrolledManually++
                }
            }
    }

    LaunchedEffect(activeIndex, userScrolledManually) {
        if (cues.isNotEmpty() && activeIndex >= 0) {
            listState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = -200
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(15.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            AdaptiveIcon(
                painter = painterResource(id = PlayerR.drawable.sync_black_24dp),
                contentDescription = stringResource(PlayerR.string.sync_subtitles),
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = stringResource(id = PlayerR.string.sync_subtitles),
                style = MaterialTheme.typography.titleMedium
                    .asAdaptiveTextStyle()
                    .copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState, snapPosition = SnapPosition.Start),
            modifier = Modifier
                .weight(1F)
                .fadingEdge(
                    scrollableState = listState,
                    orientation = Orientation.Vertical,
                    startEdge = 100.dp,
                    endEdge = 100.dp
                ),
        ) {
            itemsIndexed(
                items = cues,
                key = { index, cue -> "${index}_${cue.startTimeMs}" }
            ) { index, cue ->
                SubtitleCueItem(
                    cue = cue,
                    isActive = index == activeIndex,
                    onClick = { onCueClick(index) }
                )
            }
        }
    }
}

@Composable
private fun SubtitleCueItem(
    cue: CueWithTiming,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
        animationSpec = tween(300),
        label = "cue_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.85f,
        animationSpec = tween(300),
        label = "cue_scale"
    )

    val baseStyle = MaterialTheme.typography.titleMedium
    val style = remember(isActive) {
        if (isActive) {
            baseStyle.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            ).dropShadow()
        } else {
            baseStyle.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .noIndicationClickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = cue.cue.joinToString("\n"),
            color = textColor,
            style = style,
            modifier = Modifier.graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                transformOrigin = TransformOrigin(0f, 0.5f)
            )
        )
    }
}

@Composable
private fun OffsetControlPanel(
    currentOffset: Long,
    onOffsetChange: (Long) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .padding(15.dp)
    ) {
        Text(
            text = stringResource(id = LocaleR.string.offset),
            style = MaterialTheme.typography.titleMedium
                .asAdaptiveTextStyle()
                .copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { onOffsetChange(currentOffset - 1000) }
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = PlayerR.drawable.keyboard_double_arrow_left_thin),
                    contentDescription = stringResource(LocaleR.string.subtract_1000ms_content_description),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { onOffsetChange(currentOffset - 500) }
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = PlayerR.drawable.chevron_left_thin),
                    contentDescription = stringResource(LocaleR.string.subtract_500ms_content_description),
                    tint = Color.White
                )
            }

            AnimatedContent(
                targetState = currentOffset,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { -it / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { it / 3 } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "offset_animation",
                modifier = Modifier.weight(1F)
            ) { targetOffset ->
                Text(
                    text = "${targetOffset}ms",
                    style = MaterialTheme.typography.headlineMedium
                        .asAdaptiveTextStyle()
                        .copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                    color = Color.White
                )
            }

            IconButton(
                onClick = { onOffsetChange(currentOffset + 500) }
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = PlayerR.drawable.chevron_right_thin),
                    contentDescription = stringResource(LocaleR.string.add_500ms_content_description),
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { onOffsetChange(currentOffset + 1000) }
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = PlayerR.drawable.keyboard_double_arrow_right_thin),
                    contentDescription = stringResource(LocaleR.string.add_1000ms_content_description),
                    tint = Color.White
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            modifier = Modifier.padding(top = 20.dp)
        ) {
            TextButton(
                onClick = { onOffsetChange(0L) },
                enabled = currentOffset != 0L,
                shape = MaterialTheme.shapes.small
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = PlayerR.drawable.round_replay_24),
                    contentDescription = stringResource(LocaleR.string.reset),
                    dp = 18.dp,
                    tint = Color.White
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = stringResource(LocaleR.string.reset),
                    style = MaterialTheme.typography.bodyMedium
                        .asAdaptiveTextStyle()
                        .copy(fontWeight = FontWeight.Medium),
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = onSave,
                shape = MaterialTheme.shapes.small
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.save),
                    contentDescription = stringResource(LocaleR.string.reset),
                    dp = 18.dp,
                    tint = Color.White
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = stringResource(LocaleR.string.save),
                    style = MaterialTheme.typography.bodyMedium
                        .asAdaptiveTextStyle()
                        .copy(fontWeight = FontWeight.Medium),
                    color = Color.White
                )
            }
        }
    }
}

