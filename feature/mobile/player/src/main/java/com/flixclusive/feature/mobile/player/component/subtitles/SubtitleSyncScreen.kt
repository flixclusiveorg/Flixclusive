package com.flixclusive.feature.mobile.player.component.subtitles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.common.extensions.fadingEdge
import com.flixclusive.core.presentation.common.extensions.noOpClickable
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.player.model.CueWithTiming
import com.flixclusive.core.presentation.player.ui.state.ScrubState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.presentation.player.R as PlayerR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun SubtitleSyncScreen(
    cuesWithTiming: List<CueWithTiming>,
    currentOffset: Long,
    scrubState: ScrubState,
    onSave: (Long) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tempOffset by remember { mutableLongStateOf(currentOffset) }
    val hasUnsavedChanges by remember {
        derivedStateOf { tempOffset != currentOffset }
    }

    val initialIndex = remember {
        val exactIndex = cuesWithTiming.findCueIndex(
            position = scrubState.progress,
            offset = tempOffset,
            lastIndex = -1
        )
        if (exactIndex >= 0) exactIndex
        else cuesWithTiming.findNearestCueIndex(scrubState.progress, tempOffset)
    }

    val activeIndex = remember {
        mutableIntStateOf(initialIndex)
    }

    BackHandler {
        onDismiss()
    }

    LaunchedEffect(scrubState) {
        snapshotFlow { scrubState.progress }
            .collectLatest { playerPosition ->
                if (hasUnsavedChanges) return@collectLatest

                val newIndex = cuesWithTiming.findCueIndex(
                    position = playerPosition,
                    offset = tempOffset,
                    lastIndex = activeIndex.intValue
                )

                if (newIndex >= 0) {
                    activeIndex.intValue = newIndex
                }
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
                IconButton(onClick = onBack) {
                    AdaptiveIcon(
                        painter = painterResource(id = UiCommonR.drawable.left_arrow),
                        contentDescription = stringResource(id = LocaleR.string.navigate_up),
                        tint = Color.White
                    )
                }

                Text(
                    text = stringResource(id = PlayerR.string.sync_subtitles),
                    style = MaterialTheme.typography.headlineSmall
                        .asAdaptiveTextStyle(size = 22.sp)
                        .copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                )

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
                    cues = cuesWithTiming,
                    activeIndex = { activeIndex.intValue },
                    onCueClick = { index ->
                        val cue = cuesWithTiming[index]
                        tempOffset = scrubState.progress - cue.startTimeMs
                        activeIndex.intValue = index
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
                    hasUnsavedChanges = hasUnsavedChanges,
                    currentOffset = tempOffset,
                    onOffsetChange = {
                        tempOffset = it
                        activeIndex.intValue = cuesWithTiming.findNearestCueIndex(
                            position = scrubState.progress,
                            offset = it,
                        )
                    },
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
    activeIndex: () -> Int,
    onCueClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow(activeIndex)
            .distinctUntilChanged()
            .collectLatest { index ->
                if (cues.isEmpty()) return@collectLatest
                if (index !in cues.indices) return@collectLatest

                // If the user is scrolling, wait until they stop + debounce
                if (listState.isScrollInProgress) {
                    snapshotFlow { listState.isScrollInProgress }
                        .first { !it }
                    delay(300L)
                }


                listState.animateScrollToItem(index = index)
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState, snapPosition = SnapPosition.Start),
        modifier = modifier
            .fadingEdge(
                scrollableState = listState,
                orientation = Orientation.Vertical,
                startEdge = 50.dp,
                endEdge = 50.dp
            )
            .padding(15.dp),
    ) {
        itemsIndexed(
            items = cues,
            key = { index, cue -> "${index}_${cue.cue.firstOrNull()}" }
        ) { index, cue ->
            SubtitleCueItem(
                cue = cue,
                isActive = { index <= activeIndex() },
                onClick = { onCueClick(index) }
            )
        }
    }
}

/**
 * Finds the cue index whose adjusted time range contains [position].
 *
 * Uses O(1) sequential lookup from [lastIndex] for normal playback,
 * and falls back to O(log n) binary search on seeks or gaps.
 *
 * Assumes the list is sorted by [CueWithTiming.startTimeMs] ascending.
 *
 * @return the matching index, or -1 if no cue contains the position.
 */
private fun List<CueWithTiming>.findCueIndex(
    position: Long,
    offset: Long = 0L,
    lastIndex: Int = -1
): Int {
    if (isEmpty()) return -1

    // O(1) fast path: check if still inside the current cue
    if (lastIndex in indices) {
        val current = this[lastIndex]
        val curStart = current.startTimeMs + offset
        val curEnd = curStart + current.durationMs
        if (position in curStart..<curEnd) return lastIndex

        // O(1) fast path: check next cue (normal playback progression)
        val nextIndex = lastIndex + 1
        if (nextIndex in indices) {
            val next = this[nextIndex]
            val nextStart = next.startTimeMs + offset
            val nextEnd = nextStart + next.durationMs
            if (position in nextStart..<nextEnd) return nextIndex

            // Still in the gap between current and next cue
            if (position in curEnd..<nextStart) return -1
        } else if (position >= curEnd) {
            // Past the last cue entirely
            return -1
        }
    }

    // O(log n) fallback: binary search (seek, or no valid lastIndex)
    var low = 0
    var high = this.lastIndex
    while (low <= high) {
        val mid = (low + high) ushr 1
        val cue = this[mid]
        val adjustedStart = cue.startTimeMs + offset
        val adjustedEnd = adjustedStart + cue.durationMs

        when {
            position < adjustedStart -> high = mid - 1
            position >= adjustedEnd -> low = mid + 1
            else -> return mid
        }
    }
    return -1
}


/**
 * Finds the nearest cue index to [position], even if position is in a gap.
 * Returns the last cue that ended before or at [position], or 0 if before all cues.
 *
 * Assumes the list is sorted by [CueWithTiming.startTimeMs] ascending.
 */
private fun List<CueWithTiming>.findNearestCueIndex(
    position: Long,
    offset: Long = 0L
): Int {
    if (isEmpty()) return 0

    var low = 0
    var high = lastIndex
    while (low <= high) {
        val mid = (low + high) ushr 1
        val cue = this[mid]
        val adjustedStart = cue.startTimeMs + offset
        val adjustedEnd = adjustedStart + cue.durationMs

        when {
            position < adjustedStart -> high = mid - 1
            position >= adjustedEnd -> low = mid + 1
            else -> return mid // exact match
        }
    }

    // high = last cue that ended before position (or -1 if before all cues)
    return high.coerceIn(0, lastIndex)
}
