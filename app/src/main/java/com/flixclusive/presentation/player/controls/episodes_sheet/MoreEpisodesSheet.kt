package com.flixclusive.presentation.player.controls.episodes_sheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Season
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.presentation.common.composables.ErrorScreenWithButton
import com.flixclusive.presentation.common.composables.fadingEdge

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MoreEpisodesSheet(
    modifier: Modifier = Modifier,
    seasonData: Resource<Season>,
    availableSeasons: Int,
    currentSeasonSelected: Int,
    currentEpisodeSelected: TMDBEpisode,
    watchHistoryItem: WatchHistoryItem?,
    onSeasonChange: (Int) -> Unit,
    onEpisodeClick: (TMDBEpisode) -> Unit,
    onDismissSheet: () -> Unit,
) {
    var hasInitializedScrollToItem by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    var selectedSeason by remember { mutableStateOf(currentSeasonSelected) }
    var shouldOpenSeasonsDropdown by remember { mutableStateOf(false) }
    var episodeDetailsToShow: TMDBEpisode? by remember { mutableStateOf(null) }
    val isLongClickedEpisodeCurrentlyBeingWatched = remember(episodeDetailsToShow) {
        if(episodeDetailsToShow == null)
            return@remember false

        currentEpisodeSelected.season == episodeDetailsToShow!!.season && episodeDetailsToShow!!.episode == currentEpisodeSelected.episode
    }
    val listBottomFade = Brush.verticalGradient(0.8f to Color.Red, 0.9f to Color.Transparent)

    ScrollToItem(
        seasonData = seasonData,
        shouldOpenSeasonsDropdown = shouldOpenSeasonsDropdown,
        selectedSeason = selectedSeason,
        currentEpisodeSelected = currentEpisodeSelected,
        hasInitializedScrollToItem = hasInitializedScrollToItem,
        listState = listState,
        onInitialization = {
            hasInitializedScrollToItem = it
        }
    )


    val headerLabel = remember(shouldOpenSeasonsDropdown, episodeDetailsToShow) {
        if(shouldOpenSeasonsDropdown) {
            "Select a Season"
        } else if(episodeDetailsToShow != null) {
            "Episode Details"
        } else "Season $selectedSeason"
    }

    val dismissState = rememberDismissState(
        confirmValueChange = {
            onDismissSheet()
            true
        }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismissSheet() },
        contentAlignment = Alignment.CenterStart
    ) {
        SwipeToDismiss(
            state = dismissState,
            background = {},
            dismissContent = {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { /*Do nothing*/ }
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawRect(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black,
                                            Color.Transparent
                                        ),
                                        startX = 200F,
                                        endX = 800F
                                    )
                                )
                            }
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fadingEdge(listBottomFade)
                            .fillMaxHeight(),
                        state = listState
                    ) {
                        stickyHeader {
                            SeasonHeader(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                headerLabel = headerLabel,
                                shouldShowBackIcon = episodeDetailsToShow != null,
                                onClick = { shouldOpenSeasonsDropdown = !shouldOpenSeasonsDropdown },
                                onDismissIconClick = onDismissSheet,
                                onBackIconClick = {
                                    episodeDetailsToShow = null
                                }
                            )
                        }



                        if (seasonData is Resource.Success && !shouldOpenSeasonsDropdown && episodeDetailsToShow == null) {
                            val episodes = seasonData.data?.episodes ?: emptyList()
                            items(episodes, key = { it.episode }) { episode ->
                                SheetEpisodeItem(
                                    episode = episode,
                                    watchHistoryItem = watchHistoryItem,
                                    currentEpisodeSelected = currentEpisodeSelected,
                                    onEpisodeClick = {
                                        onEpisodeClick(it)
                                        onDismissSheet()
                                    },
                                    onEpisodeLongClick = {
                                        episodeDetailsToShow = it
                                    }
                                )
                            }
                        }

                        if (seasonData is Resource.Loading) {
                            items(5) {
                                SheetEpisodeItemPlaceholder()
                            }
                        }

                        if (seasonData is Resource.Failure) {
                            item {
                                ErrorScreenWithButton(
                                    modifier = Modifier
                                        .height(400.dp)
                                        .fillMaxWidth(),
                                    shouldShowError = true,
                                    error = "Failed to fetch season $selectedSeason",
                                    onRetry = {
                                        onSeasonChange(selectedSeason)
                                        shouldOpenSeasonsDropdown = false
                                    }
                                )
                            }
                        }

                        if(shouldOpenSeasonsDropdown) {
                            items(availableSeasons) { season ->
                                SheetSeasonItem(
                                    seasonNumber = season + 1,
                                    selectedSeason = selectedSeason,
                                    onSeasonChange = { newSeason ->
                                        selectedSeason = newSeason
                                        onSeasonChange(newSeason)
                                        shouldOpenSeasonsDropdown = false
                                    }
                                )
                            }
                        }

                        if(episodeDetailsToShow != null) {
                            sheetEpisodeDetails(
                                episode = episodeDetailsToShow!!,
                                isEpisodeCurrentlyBeingWatched = isLongClickedEpisodeCurrentlyBeingWatched,
                                watchHistoryItem = watchHistoryItem,
                                onEpisodeClick =  {
                                    onEpisodeClick(it)
                                    onDismissSheet()
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            },
            directions = setOf(DismissDirection.EndToStart)
        )
    }
}

@Composable
fun ScrollToItem(
    seasonData: Resource<Season>,
    shouldOpenSeasonsDropdown: Boolean,
    selectedSeason: Int,
    currentEpisodeSelected: TMDBEpisode,
    hasInitializedScrollToItem: Boolean,
    listState: LazyListState,
    onInitialization: (Boolean) -> Unit
) {
    LaunchedEffect(seasonData, shouldOpenSeasonsDropdown) {
        if(shouldOpenSeasonsDropdown) {
            listState.scrollToItem(selectedSeason - 1)
            return@LaunchedEffect
        }

        if(seasonData is Resource.Success && selectedSeason == currentEpisodeSelected.season && !hasInitializedScrollToItem) {
            val currentEpisodeSelectedIndex = seasonData.data!!.episodes.indexOf(currentEpisodeSelected)

            if(currentEpisodeSelectedIndex != -1) {
                listState.scrollToItem(currentEpisodeSelectedIndex)
                onInitialization(true)
            }
        } else if(seasonData is Resource.Success && selectedSeason != currentEpisodeSelected.season) {
            listState.scrollToItem(0)
            onInitialization(false)
        }
    }
}

