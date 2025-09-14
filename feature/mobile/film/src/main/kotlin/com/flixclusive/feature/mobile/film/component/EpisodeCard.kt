package com.flixclusive.feature.mobile.film.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.components.topbar.ActionButton
import com.flixclusive.core.presentation.mobile.extensions.fillMaxAdaptiveWidth
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.feature.mobile.film.R
import com.flixclusive.model.film.common.tv.Episode
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private val PreviewImageSize = 140.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EpisodeCard(
    episode: Episode,
    progress: EpisodeProgress?,
    onDownload: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloaded: Boolean = false, // TODO: Implement download state
) {
    val doHapticFeedback = getFeedbackOnLongPress()

    val downloadLabel = if (isDownloaded) {
        stringResource(R.string.downloaded)
    } else {
        stringResource(R.string.download)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    doHapticFeedback()
                    onLongClick()
                },
            )
            .padding(vertical = 8.dp, horizontal = DefaultScreenPaddingHorizontal),
    ) {
        EpisodePreview(
            episode = episode,
            episodeProgress = progress,
        )

        EpisodeDetails(
            episode = episode,
            modifier = Modifier.weight(1f),
        )

        PlainTooltipBox(description = downloadLabel) {
            ActionButton(
                onClick = onDownload,
            ) {
                AnimatedContent(isDownloaded) { state ->
                    val icon = if (state) {
                        painterResource(UiCommonR.drawable.download_done)
                    } else {
                        painterResource(UiCommonR.drawable.download)
                    }

                    AdaptiveIcon(
                        painter = icon,
                        contentDescription = stringResource(LocaleR.string.download),
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodePreview(
    episode: Episode,
    episodeProgress: EpisodeProgress?,
    modifier: Modifier = Modifier,
) {
    val imageWidthModifier = Modifier.width(getAdaptiveDp(PreviewImageSize))
    var showPlaceholder by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .padding(end = 4.dp)
            .clip(
                MaterialTheme.shapes.small.copy(
                    bottomEnd = CornerSize(0),
                    bottomStart = CornerSize(0),
                ),
            ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (showPlaceholder) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.movie_icon),
                    contentDescription = episode.title,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            FilmCover.Backdrop(
                imagePath = episode.image,
                imageSize = "w300",
                title = episode.title,
                onSuccess = { showPlaceholder = false },
                modifier = imageWidthModifier
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
            )

            if (!showPlaceholder) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .border(
                            width = 1.dp, color = Color.White.copy(0.8f),
                            shape = CircleShape
                        )
                        .background(
                            color = Color.Black.copy(0.6f),
                            shape = CircleShape
                        ),
                ) {
                    AdaptiveIcon(
                        painter = painterResource(UiCommonR.drawable.play),
                        contentDescription = stringResource(LocaleR.string.play),
                        dp = 38.dp,
                    )
                }
            }
        }

        ProgressBar(
            episodeProgress = episodeProgress,
            modifier = imageWidthModifier
                .align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun ProgressBar(
    episodeProgress: EpisodeProgress?,
    modifier: Modifier = Modifier,
) {
    val progress = remember(episodeProgress) {
        when {
            episodeProgress == null -> 0f
            episodeProgress.isFinished -> 1f
            else -> episodeProgress.progress.toFloat() / episodeProgress.duration.toFloat()
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
    ) {
        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth(progress)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun EpisodeDetails(
    episode: Episode,
    modifier: Modifier = Modifier,
) {
    val title = episode.title.trim().ifEmpty {
        stringResource(LocaleR.string.untitled_episode, episode.number)
    }

    val description = episode.overview.trim().ifEmpty {
        stringResource(R.string.default_overview)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.season_and_episode_info, episode.season, episode.number),
            style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(),
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            maxLines = 2,
        )
    }
}

@Composable
internal fun EpisodeCardPlaceholder() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Placeholder(
            modifier = Modifier
                .width(getAdaptiveDp(PreviewImageSize))
                .aspectRatio(FilmCover.Backdrop.ratio),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Placeholder(
                modifier = Modifier
                    .width(getAdaptiveDp(60.dp))
                    .height(10.dp),
            )

            Placeholder(
                modifier = Modifier
                    .width(getAdaptiveDp(120.dp))
                    .height(12.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Placeholder(
                    modifier = Modifier
                        .fillMaxAdaptiveWidth(0.8f)
                        .height(10.dp),
                )

                Placeholder(
                    modifier = Modifier
                        .fillMaxAdaptiveWidth(0.4f)
                        .height(10.dp),
                )
            }
        }

        Placeholder(
            modifier = Modifier
                .padding(end = 5.dp)
                .size(getAdaptiveDp(24.dp)),
        )
    }
}

@Preview
@Composable
private fun EpisodeCardBasePreview() {
    val series = remember { DummyDataForPreview.getTvShow() }
    val episode = remember {
        series.seasons
            .first()
            .episodes
            .first()
    }

    FlixclusiveTheme {
        Surface {
            Column {
                EpisodeCard(
                    episode = episode,
                    progress = EpisodeProgress(
                        filmId = series.identifier,
                        ownerId = -1,
                        progress = 50000L,
                        duration = 90000L,
                        seasonNumber = episode.season,
                        episodeNumber = episode.number,
                        status = WatchStatus.WATCHING,
                    ),
                    onDownload = {},
                    onClick = {},
                    onLongClick = {},
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                EpisodeCardPlaceholder()
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun EpisodeCardCompactLandscapePreview() {
    EpisodeCardBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun EpisodeCardMediumPortraitPreview() {
    EpisodeCardBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun EpisodeCardMediumLandscapePreview() {
    EpisodeCardBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun EpisodeCardExtendedPortraitPreview() {
    EpisodeCardBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun EpisodeCardExtendedLandscapePreview() {
    EpisodeCardBasePreview()
}
