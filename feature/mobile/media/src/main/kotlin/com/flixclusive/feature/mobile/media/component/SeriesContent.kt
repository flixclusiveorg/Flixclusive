package com.flixclusive.feature.mobile.media.component

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.presentation.common.components.MediaCover
import com.flixclusive.core.presentation.mobile.components.RetryButton
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.domain.provider.model.SeasonWithProgress
import com.flixclusive.model.media.common.tv.Episode
import com.flixclusive.model.media.common.tv.Season

internal fun LazyGridScope.seriesContent(
    listState: LazyListState,
    selectedSeason: Int,
    seasons: List<Season>,
    seasonToDisplay: Async<SeasonWithProgress>,
    onSeasonChange: (Season) -> Unit,
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
                    onClick = { onSeasonChange(season) },
                    modifier = Modifier
                        .padding(horizontal = 5.dp)
                        .animateItem(),
                )
            }
        }
    }

    when (seasonToDisplay) {
        is Async.Loading -> {
            items(20) {
                EpisodeCardPlaceholder()
            }
        }

        is Async.Failure -> {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val error = seasonToDisplay.message.asString()

                RetryButton(
                    error = error,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(MediaCover.Backdrop.ratio),
                )
            }
        }

        is Async.Success -> {
            val season = seasonToDisplay.data
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
                    onLongClick = onLongClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                )
            }
        }
    }
}
