package com.flixclusive.presentation.player.controls.episodes_sheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flixclusive.R
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.common.UiText
import com.flixclusive.ui.theme.colorOnMediumEmphasis


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SheetEpisodeItem(
    modifier: Modifier = Modifier,
    watchHistoryItem: WatchHistoryItem?,
    episode: TMDBEpisode,
    currentEpisodeSelected: TMDBEpisode,
    onEpisodeClick: (TMDBEpisode) -> Unit,
    onEpisodeLongClick: (TMDBEpisode) -> Unit
) {
    val context = LocalContext.current
    val cardEmphasisColor = MaterialTheme.colorScheme.primary
    val progressEmphasisColor = MaterialTheme.colorScheme.tertiary
    val cardColor = MaterialTheme.colorScheme.surfaceVariant

    val isSelected = currentEpisodeSelected.season == episode.season && currentEpisodeSelected.episode == episode.episode
    val progress = remember(watchHistoryItem) {
        if(watchHistoryItem == null)
            return@remember null

        val episodeProgress = watchHistoryItem
            .episodesWatched
            .find {
                it.episodeId == episode.episodeId
            }

        if(episodeProgress == null || episodeProgress.durationTime == 0L)
            null
        else
            episodeProgress.watchTime.toFloat() / episodeProgress.durationTime.toFloat()
    }

    val progressColor = remember {
        when(isSelected) {
            true -> progressEmphasisColor
            false -> cardEmphasisColor
        }
    }
    val textColor = remember {
        when(isSelected) {
            true -> Color.Black
            false -> Color.White
        }
    }

    Column(
        modifier = modifier.padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .height(80.dp)
                .padding(horizontal = 10.dp)
                .width(280.dp)
                .graphicsLayer {
                    shape = RoundedCornerShape(5)
                    clip = true
                }
                .drawBehind {
                    drawRect(
                        when {
                            isSelected -> cardEmphasisColor
                            else -> cardColor
                        }
                    )
                }
                .combinedClickable(
                    onLongClick = {
                        onEpisodeLongClick(episode)
                    },
                    onClick = {
                        if(!isSelected) {
                            onEpisodeClick(episode)
                        }
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .padding(10.dp)
                    .graphicsLayer {
                        shape = RoundedCornerShape(5)
                        clip = true
                    }
            ) {
                AsyncImage(
                    model = context.buildImageUrl(
                        imagePath = episode.image,
                        imageSize = "w533_and_h300_bestv2"
                    ),
                    contentDescription = "An image of episode ${episode.episode}: ${episode.title}",
                    placeholder = IconResource.fromDrawableResource(R.drawable.movie_placeholder).asPainterResource(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawRect(Color.Black.copy(0.4F))
                        }
                ) {
                    Icon(
                        painter = IconResource.fromDrawableResource(R.drawable.play)
                            .asPainterResource(),
                        contentDescription = UiText.StringResource(R.string.play_button).asString(),
                        modifier = Modifier.scale(1.5F)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1F)
                    .padding(end = 10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1F)
                ) {
                    Text(
                        text = "Episode ${episode.episode}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorOnMediumEmphasis(textColor),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 3.dp)
                    )

                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.labelMedium,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                progress?.let {
                    LinearProgressIndicator(
                        progress = it,
                        color = progressColor,
                        trackColor = progressColor.copy(0.4F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .graphicsLayer {
                                shape = RoundedCornerShape(100)
                                clip = true
                            }
                    )
                }
            }
        }
    }
}