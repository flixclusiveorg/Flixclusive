package com.flixclusive.presentation.mobile.screens.player.controls.episodes.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.presentation.mobile.common.composables.ErrorScreenWithButton
import com.flixclusive.presentation.utils.ModifierUtils.fadingEdge

@Composable
fun EpisodesRow(
    modifier: Modifier = Modifier,
    seasonData: Resource<Season>,
    selectedSeason: Int,
    currentEpisodeSelected: TMDBEpisode,
    watchHistoryItem: WatchHistoryItem?,
    onSeasonChange: (Int) -> Unit,
    onEpisodeClick: (TMDBEpisode) -> Unit,
    onClose: () -> Unit,
) {
    val listFade = Brush.horizontalGradient(
        0F to Color.Transparent,
        0.05F to Color.Red,
        0.9F to Color.Red,
        1F to Color.Transparent,
    )

    val listState = rememberLazyListState()

    LaunchedEffect(seasonData) {
        if (
            seasonData is Resource.Success
            && seasonData.data?.episodes?.contains(currentEpisodeSelected) == true
        ) {
            listState.animateScrollToItem(
                index = currentEpisodeSelected.episode - 1
            )
        } else listState.animateScrollToItem(0)
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
                .fadingEdge(listFade),
            contentPadding = PaddingValues(horizontal = 60.dp),
        ) {
            when (seasonData) {
                is Resource.Failure -> Unit
                Resource.Loading -> {
                    items(10) {
                        EpisodeCardPlaceholder()
                    }
                }
                is Resource.Success -> {
                    val episodes = seasonData.data?.episodes ?: emptyList()

                    items(
                        items = episodes,
                        key = { it.episode }
                    ) { episode ->
                        EpisodeCard(
                            data = episode,
                            watchHistoryItem = watchHistoryItem,
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

        AnimatedVisibility(
            visible = seasonData is Resource.Failure,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .height(245.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ErrorScreenWithButton(
                    modifier = Modifier
                        .matchParentSize(),
                    shouldShowError = true,
                    error = "Failed to fetch season $selectedSeason",
                    onRetry = {
                        onSeasonChange(selectedSeason)
                    }
                )
            }
        }
    }
}