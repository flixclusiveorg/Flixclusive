@file:OptIn(ExperimentalTvMaterial3Api::class, ExperimentalTvFoundationApi::class)

package com.flixclusive.feature.tv.film.component.episodes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.items
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.FilmLogo
import com.flixclusive.core.ui.tv.component.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.focusOnMount
import com.flixclusive.core.ui.tv.util.hasPressedLeft
import com.flixclusive.core.ui.tv.util.useLocalCurrentRoute
import com.flixclusive.core.ui.tv.util.useLocalLastFocusedItemPerDestination
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.model.tmdb.Season
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.model.tmdb.TvShow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val EPISODES_PANEL_FOCUS_KEY_FORMAT = "row=%d, column=%d"

@Composable
internal fun EpisodesPanel(
    film: TvShow,
    currentSelectedSeasonNumber: Int,
    currentSelectedSeason: Resource<Season>,
    onSeasonChange: (Int) -> Unit,
    onEpisodeClick: (TMDBEpisode) -> Unit,
    onHidePanel: () -> Unit,
) {
    val episodesListState = rememberTvLazyListState()

    val scope = rememberCoroutineScope()
    val topFade = Brush.verticalGradient(
        0F to Color.Transparent,
        0.16F to Color.Red
    )

    var seasonChangeJob by remember { mutableStateOf<Job?>(null) }
    var isEpisodesTabFullyFocused by remember { mutableStateOf(false) }

    var seasonName by remember { mutableStateOf("") }

    LaunchedEffect(currentSelectedSeason)
    {
        when (currentSelectedSeason) {
            is Resource.Success -> {
                seasonName = currentSelectedSeason.data?.name ?: return@LaunchedEffect
            }

            else -> return@LaunchedEffect
        }
    }

    // Initialize focus
    val lastFocusedItemMap = useLocalLastFocusedItemPerDestination()
    val currentRoute = useLocalCurrentRoute()

    LaunchedEffect(Unit) {
        // Initialize the focus on episode 1.
        val episodeToMount = 1
        lastFocusedItemMap[currentRoute] = String.format(
            EPISODES_PANEL_FOCUS_KEY_FORMAT, 1, episodeToMount
        )
    }

    BackHandler {
        onHidePanel()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.onMediumEmphasis()),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .padding(start = 100.dp, top = 50.dp)
                .onKeyEvent {
                    if (hasPressedLeft(it) && isEpisodesTabFullyFocused) {
                        onHidePanel()
                        return@onKeyEvent true
                    } else isEpisodesTabFullyFocused = true

                    false
                }
        ) {
            TvLazyColumn(
                pivotOffsets = PivotOffsets(0.16F),
                contentPadding = PaddingValues(top = LabelStartPadding.start),
                modifier = Modifier
                    .padding(top = 80.dp)
                    .fadingEdge(topFade)
                    .align(Alignment.Center)
            ) {
                item {
                    NonFocusableSpacer(height = 40.dp)
                }

                itemsIndexed(film.seasons) { i, season ->
                    val currentFocusPosition = remember { String.format(EPISODES_PANEL_FOCUS_KEY_FORMAT, 0, i) }

                    SeasonBlock(
                        seasonNumber = season.seasonNumber,
                        currentSelectedSeasonNumber = currentSelectedSeasonNumber,
                        onSeasonChange = {
                            if(seasonChangeJob?.isActive == true)
                                seasonChangeJob?.cancel()

                            seasonChangeJob = scope.launch {
                                delay(800)
                                onSeasonChange(season.seasonNumber)
                                episodesListState.scrollToItem(0)
                            }
                        },
                        modifier = Modifier
                            .focusOnMount(itemKey = currentFocusPosition)
                    )
                }

                items(10) {
                    NonFocusableSpacer(height = 40.dp)
                }
            }

            FilmLogo(
                film = film,
                showTitleOnError = false,
                alignment = Alignment.Center,
                modifier = Modifier
                    .size(height = 80.dp, width = 200.dp)
                    .align(Alignment.TopCenter)
            )
        }

        TvLazyColumn(
            pivotOffsets = PivotOffsets(0.13F),
            state = episodesListState,
            modifier = Modifier
                .weight(1F)
                .fillMaxHeight()
                .onPreviewKeyEvent {
                    isEpisodesTabFullyFocused = false
                    false
                }
        ) {
            stickyHeader {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.onMediumEmphasis(emphasis = 0.2F)
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = seasonName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        ),
                        modifier = Modifier
                            .padding(25.dp)
                    )
                }
            }

            if (currentSelectedSeason is Resource.Success) {
                items(currentSelectedSeason.data!!.episodes) { episode ->
                    val currentFocusPosition = remember { String.format(EPISODES_PANEL_FOCUS_KEY_FORMAT, 1, episode.episode) }

                    EpisodeCard(
                        episode = episode,
                        onEpisodeClick = { onEpisodeClick(episode) },
                        modifier = Modifier
                            .focusOnMount(itemKey = currentFocusPosition)
                    )
                }
            }

            if (currentSelectedSeason is Resource.Loading) {
                items(5) {
                    EpisodeItemPlaceholder()
                }
            }
        }
    }
}