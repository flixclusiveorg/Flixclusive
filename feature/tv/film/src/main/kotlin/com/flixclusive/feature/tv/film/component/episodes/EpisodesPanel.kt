@file:OptIn(ExperimentalTvMaterial3Api::class, ExperimentalTvFoundationApi::class)

package com.flixclusive.feature.tv.film.component.episodes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.fadingEdge
import com.flixclusive.core.ui.common.util.ifElse
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.FilmLogo
import com.flixclusive.core.ui.tv.component.NonFocusableSpacer
import com.flixclusive.core.ui.tv.util.FocusRequesterModifiers
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.createInitialFocusRestorerModifiers
import com.flixclusive.core.ui.tv.util.focusOnMount
import com.flixclusive.core.ui.tv.util.hasPressedLeft
import com.flixclusive.core.ui.tv.util.useLocalCurrentRoute
import com.flixclusive.core.ui.tv.util.useLocalLastFocusedItemPerDestination
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.exception.safeCall
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.TvShow

private const val EPISODES_PANEL_FOCUS_KEY_FORMAT = "row=%d, column=%d"

@Composable
internal fun EpisodesPanel(
    film: TvShow,
    currentSelectedSeasonNumber: Int,
    currentSelectedSeason: Resource<Season>,
    onSeasonChange: (Int) -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onHidePanel: () -> Unit,
) {
    val episodesListState = rememberTvLazyListState()

    val topFade = Brush.verticalGradient(
        0F to Color.Transparent,
        0.16F to Color.Red
    )

    var isEpisodesTabFullyFocused by remember { mutableStateOf(false) }
    var firstEpisodeCard by remember { mutableIntStateOf(episodesListState.firstVisibleItemIndex) }

    var seasonName by remember { mutableStateOf("") }

    LaunchedEffect(currentSelectedSeason)
    {
        safeCall {
            episodesListState.scrollToItem(0)
            firstEpisodeCard = episodesListState.firstVisibleItemIndex
        }
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

    val seasonsFocusModifiers = createInitialFocusRestorerModifiers()
    val episodesFocusModifiers = createEpisodesPanelFocusRestorers(currentSelectedSeason)

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
                .focusGroup()
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
                modifier = seasonsFocusModifiers.parentModifier
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
                        seasonNumber = season.number,
                        currentSelectedSeasonNumber = currentSelectedSeasonNumber,
                        onSeasonChange = {
                            onSeasonChange(season.number)
                        },
                        modifier = Modifier
                            .focusOnMount(itemKey = currentFocusPosition)
                            .ifElse(
                                condition = i == 0,
                                ifTrueModifier = seasonsFocusModifiers.childModifier
                            )
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
            modifier = episodesFocusModifiers.parentModifier
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
                itemsIndexed(currentSelectedSeason.data!!.episodes) { i, episode ->
                    val currentFocusPosition = remember { String.format(EPISODES_PANEL_FOCUS_KEY_FORMAT, 1, episode.number) }

                    EpisodeCard(
                        episode = episode,
                        onEpisodeClick = { onEpisodeClick(episode) },
                        modifier = Modifier
                            .focusOnMount(itemKey = currentFocusPosition)
                            .ifElse(
                                condition = i == firstEpisodeCard,
                                ifTrueModifier = episodesFocusModifiers.childModifier
                            )
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

/**
 *
 * Just a cheap workaround for [createInitialFocusRestorerModifiers]
 * to focus initially on the first episode whenever the season state
 * changes.
 * */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun createEpisodesPanelFocusRestorers(
    currentSelectedSeason: Resource<Season>,
): FocusRequesterModifiers {
    var shouldFocusInitialChild by remember { mutableStateOf(false) }

    LaunchedEffect(currentSelectedSeason) {
        shouldFocusInitialChild = true
    }

    val focusRequester = remember { FocusRequester() }
    val childFocusRequester = remember { FocusRequester() }

    val parentModifier = Modifier
        .focusRequester(focusRequester)
        .focusProperties {
            exit = {
                focusRequester.saveFocusedChild()
                FocusRequester.Default
            }
            enter = {
                if (shouldFocusInitialChild) {
                    shouldFocusInitialChild = false
                    childFocusRequester
                } else {
                    // Safe call because this one's still bugged.
                    val isRestored = safeCall {
                        focusRequester.restoreFocusedChild()
                    }

                    when (isRestored) {
                        true -> FocusRequester.Cancel
                        null -> FocusRequester.Default // Fail-safe if compose tv acts up
                        else -> childFocusRequester
                    }
                }
            }
        }

    val childModifier = Modifier.focusRequester(childFocusRequester)

    return FocusRequesterModifiers(
        parentModifier = parentModifier,
        childModifier = childModifier
    )
}