package com.flixclusive.feature.mobile.player.component.episodes.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.presentation.common.extensions.fadingEdge
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.film.common.tv.Season
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
internal fun SeasonsRow(
    seasons: List<Season>,
    currentSeason: () -> Season?,
    onSeasonChange: (Season) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        currentSeason()?.number?.let {
            safeCall { listState.animateScrollToItem(max(0, it - 1)) }
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = modifier
            .fillMaxWidth()
            .fadingEdge(
                scrollableState = listState,
                orientation = Orientation.Horizontal,
                edgeSize = 100.dp
            )
    ) {
        items(seasons) { season ->
            SeasonPill(
                season = season,
                selected = { currentSeason()?.number == season.number },
                onClick = {
                    scope.launch {
                        onSeasonChange(season)
                        safeCall {
                            listState.animateScrollToItem(season.number - 1)
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
                    seasons = List(5) {
                        Season(
                            number = it + 1,
                            episodeCount = 10,
                        )
                    },
                    currentSeason = {
                        Season(
                            number = 2,
                            episodeCount = 10,
                        )
                    },
                    onSeasonChange = {}
                )
            }
        }
    }
}
