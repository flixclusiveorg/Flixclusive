@file:OptIn(ExperimentalTvMaterial3Api::class, ExperimentalTvFoundationApi::class)

package com.flixclusive.feature.tv.film.component.episodes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.component.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.createInitialFocusRestorerModifiers
import com.flixclusive.core.ui.tv.util.focusOnMount
import com.flixclusive.core.ui.tv.util.hasPressedLeft
import com.flixclusive.core.util.common.resource.Resource
import com.flixclusive.model.tmdb.Season
import com.flixclusive.model.tmdb.TMDBEpisode
import com.flixclusive.model.tmdb.TvShow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun EpisodesPanel(
    isVisible: Boolean,
    film: TvShow,
    currentSelectedSeasonNumber: Int,
    currentSelectedSeason: Resource<Season>,
    onSeasonChange: (Int) -> Unit,
    onEpisodeClick: (TMDBEpisode) -> Unit,
    onHidePanel: () -> Unit,
) {
    val seasonsListState = rememberTvLazyListState()
    val episodesListState = rememberTvLazyListState()

    val scope = rememberCoroutineScope()
    val topFade = Brush.verticalGradient(
        0F to Color.Transparent,
        0.16F to Color.Red
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val seasonsFocusRequesterModifier = createInitialFocusRestorerModifiers()
        var seasonsTabHasFocus by remember { mutableStateOf(false) }

        val seasonsInitialFocus by remember { mutableIntStateOf(seasonsListState.firstVisibleItemIndex) }

        var seasonName by remember { mutableStateOf("") }

        LaunchedEffect(currentSelectedSeason) {
            when (currentSelectedSeason) {
                is Resource.Success -> {
                    seasonName = currentSelectedSeason.data?.name ?: return@LaunchedEffect
                }

                else -> return@LaunchedEffect
            }
        }

        BackHandler {
            onHidePanel()
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.onMediumEmphasis())
                .onPreviewKeyEvent {
                    if (hasPressedLeft(it) && seasonsTabHasFocus) {
                        onHidePanel()
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TvLazyColumn(
                pivotOffsets = PivotOffsets(0.16F),
                contentPadding = PaddingValues(top = LabelStartPadding.start),
                state = seasonsListState,
                modifier = seasonsFocusRequesterModifier.parentModifier
                    .padding(start = 100.dp, top = 100.dp)
                    .fadingEdge(topFade)
                    .onFocusChanged {
                        scope.launch {
                            seasonsTabHasFocus = if (it.hasFocus) {
                                delay(500)
                                true
                            } else false
                        }
                    }
            ) {
                item {
                    NonFocusableSpacer(height = 40.dp)
                }

                itemsIndexed(
                    film.seasons
                ) { i, season ->
                    val currentFocusPosition = remember { "row=0, column=$i" }

                    SeasonBlock(
                        seasonNumber = season.seasonNumber,
                        currentSelectedSeasonNumber = currentSelectedSeasonNumber,
                        onSeasonChange = {
                            scope.launch {
                                delay(800)
                                onSeasonChange(season.seasonNumber)
                                episodesListState.scrollToItem(0)
                            }
                        },
                        modifier = Modifier
                            .ifElse(
                                condition = i == seasonsInitialFocus,
                                ifTrueModifier = seasonsFocusRequesterModifier.childModifier
                            )
                            .focusOnMount(itemKey = currentFocusPosition)
                    )
                }

                items(10) {
                    NonFocusableSpacer(height = 40.dp)
                }
            }

            TvLazyColumn(
                pivotOffsets = PivotOffsets(0.13F),
                state = episodesListState,
                modifier = Modifier
                    .weight(1F)
                    .fillMaxHeight()
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
                        val currentFocusPosition = remember { "row=1, column=$${episode.episode}" }

                        EpisodeCard(
                            episode = episode,
                            onEpisodeClick = { onEpisodeClick(episode) },
                            modifier = Modifier.focusOnMount(itemKey = currentFocusPosition)
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
}