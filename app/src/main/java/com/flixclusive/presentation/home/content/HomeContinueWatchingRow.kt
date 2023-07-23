package com.flixclusive.presentation.home.content

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flixclusive.R
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.common.Formatter.formatMinutes
import com.flixclusive.presentation.common.Functions.getNextEpisodeToWatch
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.main.LABEL_START_PADDING

@Composable
fun HomeContinueWatchingRow(
    modifier: Modifier = Modifier,
    dataListProvider: () -> List<WatchHistoryItem>,
    onFilmClick: (Film) -> Unit,
    onSeeMoreClick: (Film) -> Unit,
) {
    if(dataListProvider().isNotEmpty()) {
        Column(
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 25.dp)
                    .graphicsLayer {
                        shape = RoundedCornerShape(10)
                        clip = true
                    }
            ) {
                Text(
                    text = "Continue Watching",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1F)
                        .padding(start = LABEL_START_PADDING)
                )
            }


            LazyRow {
                itemsIndexed(
                    items = dataListProvider(),
                    key = { i, film ->
                        film.id * i
                    }
                ) { _, item ->
                    HomeContinueWatchingItem(
                        modifier = Modifier
                            .width(135.dp),
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
fun HomeContinueWatchingItem(
    watchHistoryItem: WatchHistoryItem,
    onClick: (Film) -> Unit,
    onSeeMoreClick: (Film) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
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

        mutableStateOf(percentage)
    }

    val posterImage: ImageRequest = context.buildImageUrl(
        imagePath = film.posterImage,
        imageSize = "w220_and_h330_face"
    )

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 3.dp)
            .combinedClickable(
                onClick = { onClick(film) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSeeMoreClick(film)
                }
            )
    ) {
        Box(
            modifier = Modifier
                .height(200.dp)
                .padding(3.dp)
                .graphicsLayer {
                    shape = RoundedCornerShape(5)
                    clip = true
                }
        ) {
            AsyncImage(
                model = posterImage,
                placeholder = IconResource.fromDrawableResource(R.drawable.movie_placeholder)
                    .asPainterResource(),
                contentDescription = UiText.StringResource(R.string.film_item_content_description).asString(),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black
                                ),
                                endY = size.height.times(0.9F)
                            )
                        )
                    }
            ) {
                val itemLabel = remember(watchHistoryItem) {
                    if(isTvShow) {
                        val nextEpisodeWatched = getNextEpisodeToWatch(watchHistoryItem)
                        val season = nextEpisodeWatched.first
                        val episode = nextEpisodeWatched.second

                        val lastEpisodeIsNotSameWithNextEpisodeToWatch = lastWatchedEpisode.episodeNumber != episode

                        if(lastEpisodeIsNotSameWithNextEpisodeToWatch)
                            progress = 0F

                        "S${season} E${episode}"
                    } else {
                        val watchTime = watchHistoryItem.episodesWatched.last().watchTime
                        val watchTimeInSeconds = (watchTime / 1000).toInt()
                        val watchTimeInMinutes = watchTimeInSeconds / 60

                        formatMinutes(totalMinutes = watchTimeInMinutes)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .border(1.dp, Color.White, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.play),
                        contentDescription = "A play button icon",
                        modifier = Modifier
                            .size(50.dp),
                        tint = Color.White
                    )
                }

                Text(
                    text = itemLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            bottom = 8.dp,
                        )
                )
            }

            IconButton(
                onClick = { onSeeMoreClick(film) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.round_more_vert_24),
                    contentDescription = "A vertical see more button icon",
                    tint = Color.White
                )
            }
        }

        LinearProgressIndicator(
            progress = progress,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(0.4F),
            modifier = Modifier
                .padding(5.dp)
                .graphicsLayer {
                    shape = RoundedCornerShape(25)
                    clip = true
                }
        )
    }
}