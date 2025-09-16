package com.flixclusive.feature.mobile.film.component

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.model.film.common.tv.Season
import com.flixclusive.core.strings.R as LocaleR

internal fun LazyGridScope.seriesContent(
    listState: LazyListState,
    selectedSeason: Int,
    longClickedEpisode: EpisodeWithProgress?,
    seasons: List<Season>,
    seasonToDisplay: Resource<SeasonWithProgress>,
    onSeasonChange: (Int) -> Unit,
    onClick: (Episode) -> Unit,
    onLongClick: (EpisodeWithProgress) -> Unit,
    onRetry: () -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        LazyRow(
            state = listState,
            modifier = Modifier.padding(bottom = 5.dp),
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
                            end = DefaultScreenPaddingHorizontal,
                        ),
                        style = MaterialTheme.typography.bodySmall
                            .copy(color = LocalContentColor.current.copy(0.6f))
                            .asAdaptiveTextStyle(),
                    )
                }
            }

            items(
                items = season.episodes,
                key = { episode -> episode.number },
            ) { item ->
                EpisodeCard(
                    episode = item,
                    onClick = { onClick(item.episode) },
                    visible = longClickedEpisode != item,
                    onLongClick = { onLongClick(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }
    }
}
