package com.flixclusive.feature.mobile.player.component.episode.component

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import com.flixclusive.core.presentation.common.extensions.fadingEdge
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.model.film.common.tv.Episode

@Composable
internal fun EpisodesRow(
    modifier: Modifier = Modifier,
    seasonData: () -> SeasonWithProgress?,
    currentEpisodeSelected: Episode,
    onEpisodeClick: (Episode) -> Unit,
    onClose: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(seasonData()) {
        safeCall {
            if (seasonData()?.episodes?.fastAny { it == currentEpisodeSelected } == true) {
                listState.animateScrollToItem(
                    index = currentEpisodeSelected.number - 1
                )
            } else listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        LazyRow(
            state = listState,
            modifier = modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .fadingEdge(
                    scrollableState = listState,
                    orientation = Orientation.Horizontal,
                    edgeSize = 150.dp
                ),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            items(seasonData()?.episodes ?: emptyList()) { episode ->
                EpisodeCard(
                    data = episode,
                    currentEpisodeSelected = currentEpisodeSelected,
                    onEpisodeClick = {
                        onEpisodeClick(it)
                        onClose()
                    }
                )
            }
        }
    }
}
