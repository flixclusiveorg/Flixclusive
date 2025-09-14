package com.flixclusive.feature.mobile.film.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.core.strings.R as LocaleR

/**
 * Returns the [EpisodeProgress] for the given [episode], or null if not found.
 *
 * @param episode The episode to find the progress for.
 *
 * @return The [EpisodeProgress] for the given [episode], or null if not.
 * */
private fun List<EpisodeProgress>.getEpisode(episode: Episode) =
    fastFirstOrNull {
        it.seasonNumber == episode.season &&
            it.episodeNumber == episode.number
    }

internal fun LazyGridScope.seriesContent(
    listState: LazyListState,
    selectedSeason: Int,
    seasons: List<Season>,
    progresses: List<EpisodeProgress>,
    seasonToDisplay: Resource<Season>,
    onSeasonChange: (Int) -> Unit,
    onDownload: (Episode) -> Unit,
    onClick: (Episode) -> Unit,
    onLongClick: (Episode) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        LazyRow(
            state = listState,
            modifier = Modifier.padding(bottom = 5.dp)
        ) {
            items(
                items = seasons,
                key = { season -> season.number },
            ) { season ->
                SeasonPill(
                    season = season,
                    selected = selectedSeason == season.number,
                    onClick = { onSeasonChange(season.number) },
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .animateItem(),
                )
            }
        }
    }

    when {
        seasonToDisplay is Resource.Loading -> {
            items(20) {
                EpisodeCardPlaceholder()
            }
        }

        seasonToDisplay is Resource.Failure || seasonToDisplay.data == null -> {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val error = if (seasonToDisplay.error != null) {
                    seasonToDisplay.error!!.asString()
                } else {
                    stringResource(LocaleR.string.season_fetch_error, selectedSeason)
                }

                RetryButton(
                    error = error,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(FilmCover.Backdrop.ratio),
                )
            }
        }

        seasonToDisplay is Resource.Success -> {
            val season = seasonToDisplay.data!!
            season.overview?.let {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = it,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(
                            bottom = 10.dp,
                            start = DefaultScreenPaddingHorizontal,
                            end = DefaultScreenPaddingHorizontal
                        ),
                        style = MaterialTheme.typography.bodySmall
                            .copy(color = LocalContentColor.current.copy(0.6f))
                            .asAdaptiveTextStyle()
                    )
                }
            }

            itemsIndexed(
                items = season.episodes,
                key = { _, episode -> episode.number },
            ) { i, episode ->
                val progress = remember(progresses) { progresses.getEpisode(episode) }

                Column {
                    EpisodeCard(
                        episode = episode,
                        onClick = { onClick(episode) },
                        onLongClick = { onLongClick(episode) },
                        onDownload = { onDownload(episode) },
                        progress = progress,
                        modifier = modifier
                            .fillMaxWidth()
                            .animateItem(),
                    )

                    if (i < season.episodes.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                            modifier = Modifier
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                        )
                    }
                }
            }
        }
    }
}
