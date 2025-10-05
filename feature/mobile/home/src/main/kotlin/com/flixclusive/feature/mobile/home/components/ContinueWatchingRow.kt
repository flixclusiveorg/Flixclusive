package com.flixclusive.feature.mobile.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.common.locale.UiText
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.EpisodeProgressWithMetadata
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.MovieProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchProgressWithMetadata
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.FilmFormatterUtil.formatAsRuntime
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.getAdaptiveFilmCardWidth
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.feature.mobile.home.R
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.util.FilmType
import kotlin.math.roundToLong
import kotlin.random.Random
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun ContinueWatchingRow(
    showCardTitle: Boolean,
    items: List<WatchProgressWithMetadata>,
    onItemClick: (WatchProgressWithMetadata) -> Unit,
    onSeeMoreClick: (Film) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = 25.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(LocaleR.string.continue_watching),
            style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(
                size = 16.sp,
                increaseBy = 10.sp
            ),
            modifier = Modifier
                .padding(start = 15.dp)
        )

        LazyRow {
            items(
                items = items,
                key = { it.id }
            ) { item ->
                ContinueWatchingCard(
                    showTitle = showCardTitle,
                    item = item,
                    onClick = { onItemClick(item) },
                    onSeeMoreClick = { onSeeMoreClick(item.film) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingCard(
    item: WatchProgressWithMetadata,
    showTitle: Boolean,
    onClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val feedbackOnLongPress = getFeedbackOnLongPress()

    val film = remember(item) { item.film }
    val progress = remember(item) {
        val percentage = if (item.watchData.progress == 0L) {
            0F
        } else {
            item.watchData.progress.toFloat() / item.watchData.duration.toFloat()
        }

        percentage.coerceIn(0f, 1f)
    }

    val itemLabel = remember(item) {
        if(item is EpisodeProgressWithMetadata) {
            UiText.from("S${item.watchData.seasonNumber} E${item.watchData.episodeNumber}")
        } else {
            val watchTime = item.watchData.progress
            val watchTimeInSeconds = (watchTime / 1000).toInt()
            val watchTimeInMinutes = watchTimeInSeconds / 60

            watchTimeInMinutes.formatAsRuntime()
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
                .background(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        feedbackOnLongPress()
                        onSeeMoreClick()
                    }
                )
        ) {
            FilmCover.Poster(
                imagePath = film.posterImage,
                title = film.title,
                imageSize = "w300",
                modifier = Modifier.width(getAdaptiveFilmCardWidth())
            )

            Box(
                modifier = Modifier
                    .size(getAdaptiveDp(50.dp, 15.dp))
                    .align(Alignment.Center)
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .background(
                        color = Color.Black.copy(0.6f),
                        shape = CircleShape
                    )
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.play),
                    contentDescription = stringResource(id = LocaleR.string.play),
                    tint = Color.White,
                    dp = 30.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0F to Color.Transparent,
                            0.6F to Color.Transparent,
                            0.95F to MaterialTheme.colorScheme.surface.copy(alpha = 0.8F)
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
                        style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
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
                onClick = onSeeMoreClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
            ) {
                AdaptiveIcon(
                    painter = painterResource(id = R.drawable.round_more_vert_24),
                    contentDescription = stringResource(LocaleR.string.see_more_btn_content_desc)
                )
            }
        }

        if(showTitle) {
            Text(
                text = film.title,
                style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                color = LocalContentColor.current.copy(alpha = 0.8F),
                maxLines = 1,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ContinueWatchingRowBasePreview() {
    val items = remember {
        List(20) { index ->
            val film = DummyDataForPreview.getFilm(
                id = "film_$index",
                title = "Sample Film ${index + 1}",
                filmType = if (index % 2 == 0) FilmType.MOVIE else FilmType.TV_SHOW,
            ).toDBFilm()

            val ownerId = index
            val duration = Random.nextLong(2, 3) * 1000 * 60 * 60
            val progress = (duration.toDouble() * Random.nextDouble(0.3, 0.9)).roundToLong()
            val watchStatus = WatchStatus.WATCHING

            if (index % 2 == 0) {
                MovieProgressWithMetadata(
                    film = film,
                    watchData = MovieProgress(
                        id = index.toLong(),
                        ownerId = ownerId,
                        progress = progress,
                        duration = duration,
                        status = watchStatus,
                        filmId = film.identifier
                    )
                )
            } else {
                EpisodeProgressWithMetadata(
                    film = film,
                    watchData = EpisodeProgress(
                        id = index.toLong(),
                        ownerId = ownerId,
                        progress = progress,
                        duration = duration,
                        status = watchStatus,
                        episodeNumber = Random.nextInt(24),
                        seasonNumber = Random.nextInt(10),
                        filmId = film.identifier
                    )
                )
            }
        }
    }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            ContinueWatchingRow(
                items = items,
                showCardTitle = false,
                onItemClick = {},
                onSeeMoreClick = {}
            )
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun ContinueWatchingRowCompactLandscapePreview() {
    ContinueWatchingRowBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun ContinueWatchingRowMediumPortraitPreview() {
    ContinueWatchingRowBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun ContinueWatchingRowMediumLandscapePreview() {
    ContinueWatchingRowBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun ContinueWatchingRowExtendedPortraitPreview() {
    ContinueWatchingRowBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun ContinueWatchingRowExtendedLandscapePreview() {
    ContinueWatchingRowBasePreview()
}
