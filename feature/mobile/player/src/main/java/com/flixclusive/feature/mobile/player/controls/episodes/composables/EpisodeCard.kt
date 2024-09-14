package com.flixclusive.feature.mobile.player.controls.episodes.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.FilmCover
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.placeholderEffect
import com.flixclusive.model.database.EpisodeWatched
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.film.common.tv.Episode
import kotlin.random.Random
import com.flixclusive.core.ui.common.R as UiCommonR
import com.flixclusive.core.locale.R as LocaleR

@Composable
internal fun EpisodeCard(
    modifier: Modifier = Modifier,
    data: Episode,
    watchHistoryItem: WatchHistoryItem?,
    currentEpisodeSelected: Episode,
    onEpisodeClick: (Episode) -> Unit,
) {
    val title = remember { "${data.number}. ${data.title}" }

    val cardEmphasisColor = MaterialTheme.colorScheme.primary
    val progressEmphasisColor = MaterialTheme.colorScheme.tertiary

    val isSelected = currentEpisodeSelected == data
    val progress = remember(watchHistoryItem) {
        if (watchHistoryItem == null)
            return@remember null

        val episodeProgress = watchHistoryItem
            .episodesWatched
            .find {
                it.episodeId == data.id
            }

        when {
            isSelected -> 0F
            episodeProgress == null || episodeProgress.durationTime == 0L -> null
            else -> episodeProgress.watchTime.toFloat() / episodeProgress.durationTime.toFloat()
        }
    }

    val overlayColor = Brush.verticalGradient(
        0F to Color.Transparent,
        0.6F to Color.Transparent,
        0.95F to if (isSelected) MaterialTheme.colorScheme.primary.onMediumEmphasis(emphasis = 0.8F) else MaterialTheme.colorScheme.surface.onMediumEmphasis(emphasis = 0.8F)
    )
    val progressColor = remember {
        when (isSelected) {
            true -> progressEmphasisColor
            false -> cardEmphasisColor
        }
    }

    Column(
        modifier = modifier
            .width(220.dp)
            .padding(vertical = 5.dp, horizontal = 10.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .clickable(enabled = !isSelected) {
                onEpisodeClick(data)
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable(enabled = !isSelected) {
                    onEpisodeClick(data)
                },
        ) {
            FilmCover.Backdrop(
                imagePath = data.image,
                imageSize = "w227_and_h127_bestv2",
                showPlaceholder = false,
                contentDescription = stringResource(
                    LocaleR.string.episode_image_content_desc_format,
                    data.number,
                    data.title
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )

            if (!isSelected) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
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
                            .size(20.dp)
                            .align(Alignment.Center),
                        tint = Color.White
                    )
                }
            }

            progress?.let {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(overlayColor)
                        .clip(MaterialTheme.shapes.extraSmall)
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        color = progressColor,
                        trackColor = progressColor.copy(0.2F),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .padding(8.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .heightIn(min = 35.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 2
            )
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = LocalContentColor.current.onMediumEmphasis()
        )

        Box(
            modifier = Modifier
                .height(65.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = data.overview,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalContentColor.current.onMediumEmphasis()
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
internal fun EpisodeCardPlaceholder(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(220.dp)
            .padding(vertical = 5.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(FilmCover.Backdrop.ratio)
                    .placeholderEffect()
            )
        }

        Spacer(
            modifier = Modifier
                .width(100.dp)
                .height(14.dp)
                .placeholderEffect()
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = LocalContentColor.current.onMediumEmphasis()
        )

        for (i in 0..3) {
            val number = remember { Random.nextDouble(0.9, 1.0).toFloat() }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth(number)
                    .height(9.dp)
                    .placeholderEffect()
            )
        }
    }
}

@Preview(
    device = "spec:parent=pixel_5,orientation=landscape",
    showSystemUi = true,
)
@Composable
private fun EpisodeCardPreview() {
    val sampleWatchHistoryItem = WatchHistoryItem(
        episodesWatched = listOf(
            EpisodeWatched(
                "",
                seasonNumber = 1,
                episodeNumber = 1,
                watchTime = 50000L,
                durationTime = 600000L,
                isFinished = false
            )
        )
    )
    val sampleEpisode = Episode(
        number = 1,
        season = 1,
        title = "Sample Title",
        overview = "A car explodes in the middle of the city and kills a person. The cause of the explosion is unknown. Cha Jae Hwan, a detective from the Violent Crimes Unit, notices something strange at the scene. Eugene Hathaway, an FBI agent on duty in Korea, helps him take another look. Later, Jae Hwan chases after a suspect, and in the end, he gets faced with a dark figure.",
        image = "/eDXV3upl9iXqOtYaNuzAfZvmfTU.jpg"
    )

    FlixclusiveTheme {
        Surface {
            Row {
                EpisodeCard(
                    data = sampleEpisode,
                    watchHistoryItem = sampleWatchHistoryItem,
                    currentEpisodeSelected = Episode(),
                    onEpisodeClick = { _ -> }
                )

                EpisodeCardPlaceholder(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}