package com.flixclusive.feature.mobile.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.locale.UiText
import com.flixclusive.core.ui.common.FilmCover
import com.flixclusive.core.ui.common.util.formatMinutes
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.film.Film
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

@Composable
internal fun HomeContinueWatchingRow(
    modifier: Modifier = Modifier,
    showCardTitle: Boolean,
    dataListProvider: () -> List<WatchHistoryItem>,
    onFilmClick: (Film) -> Unit,
    onSeeMoreClick: (Film) -> Unit,
) {
    if(dataListProvider().isNotEmpty()) {
        Column(
            modifier = modifier
                .padding(top = 25.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(LocaleR.string.continue_watching),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(start = 15.dp)
            )


            LazyRow {
                items(
                    items = dataListProvider(),
                    key = { it.id }
                ) { item ->
                    ContinueWatchingCard(
                        modifier = Modifier
                            .width(135.dp),
                        showCardTitle = showCardTitle,
                        watchHistoryItem = item,
                        onClick = onFilmClick,
                        onSeeMoreClick = { onSeeMoreClick(item.film) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingCard(
    watchHistoryItem: WatchHistoryItem,
    showCardTitle: Boolean,
    onClick: (Film) -> Unit,
    onSeeMoreClick: (Film) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val film = watchHistoryItem.film

    val isTvShow = watchHistoryItem.seasons != null

    if(watchHistoryItem.episodesWatched.isEmpty())
        return

    val lastWatchedEpisode = watchHistoryItem.episodesWatched.last()
    var progress by remember(watchHistoryItem) {
        val percentage = if(lastWatchedEpisode.durationTime == 0L) {
            0F
        } else {
            lastWatchedEpisode.watchTime.toFloat() / lastWatchedEpisode.durationTime.toFloat()
        }

        mutableFloatStateOf(percentage)
    }

    val itemLabel = remember(watchHistoryItem) {
        if(isTvShow) {
            val nextEpisodeWatched = getNextEpisodeToWatch(watchHistoryItem)
            val season = nextEpisodeWatched.first
            val episode = nextEpisodeWatched.second

            val lastEpisodeIsNotSameWithNextEpisodeToWatch = lastWatchedEpisode.episodeNumber != episode

            if(lastEpisodeIsNotSameWithNextEpisodeToWatch)
                progress = 0F

            UiText.StringValue("S${season} E${episode}")
        } else {
            val watchTime = watchHistoryItem.episodesWatched.last().watchTime
            val watchTimeInSeconds = (watchTime / 1000).toInt()
            val watchTimeInMinutes = watchTimeInSeconds / 60

            formatMinutes(totalMinutes = watchTimeInMinutes)
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .combinedClickable(
                    onClick = { onClick(film) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSeeMoreClick(film)
                    }
                )
        ) {
            FilmCover.Poster(
                imagePath = film.posterImage,
                showPlaceholder = false,
                imageSize = "w300"
            )

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center)
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .background(
                        color = Color.Black.onMediumEmphasis(),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    painter = painterResource(id = UiCommonR.drawable.play),
                    contentDescription = stringResource(id = LocaleR.string.play_button),
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.Center),
                    tint = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0F to Color.Transparent,
                            0.6F to Color.Transparent,
                            0.95F to MaterialTheme.colorScheme.surface.onMediumEmphasis(emphasis = 0.8F)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = itemLabel.asString(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp
                        ),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.clip(MaterialTheme.shapes.large),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(0.4F),
                        drawStopIndicator = {}
                    )
                }
            }

            IconButton(
                onClick = { onSeeMoreClick(film) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.round_more_vert_24),
                    contentDescription = stringResource(LocaleR.string.see_more_btn_content_desc),
                    tint = Color.White
                )
            }
        }

        if(showCardTitle) {
            Text(
                text = film.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp
                ),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                color = LocalContentColor.current.onMediumEmphasis(emphasis = 0.8F),
                maxLines = 1,
                modifier = Modifier
                    .padding(vertical = 5.dp)
            )
        }
    }
}