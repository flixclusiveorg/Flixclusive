package com.flixclusive.feature.mobile.player.controls.episodes.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.util.exception.safeCall
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
internal fun SeasonsRow(
    availableSeasons: Int,
    currentSeasonSelected: Int?,
    onSeasonChange: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val (selectedIndex, onIndexChange) = remember { mutableStateOf(currentSeasonSelected) }

    val listFade = Brush.horizontalGradient(
        0F to Color.Transparent,
        0.05F to Color.Red,
        0.9F to Color.Red,
        1F to Color.Transparent,
    )

    LaunchedEffect(Unit) {
        selectedIndex?.let {
            safeCall { listState.animateScrollToItem(max(0, it - 1)) }
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        contentPadding = PaddingValues(start = 65.dp, end = 85.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fadingEdge(listFade)
    ) {
        items(availableSeasons) { i ->
            val season = remember { i + 1 }

            SeasonPill(
                season = season,
                selected = season == selectedIndex,
                onClick = {
                    scope.launch {
                        onIndexChange(season)
                        onSeasonChange(season)
                        safeCall {
                            listState.animateScrollToItem(i)
                        }
                    }
                }
            )
        }
    }
}

@Preview(
    device = "spec:parent=pixel_5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun SeasonsRowPreview() {
    FlixclusiveTheme {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SeasonsRow(
                    availableSeasons = 14,
                    currentSeasonSelected = 1,
                    onSeasonChange = {}
                )
            }
        }
    }
}