package com.flixclusive.feature.mobile.film.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.FilmFormatterUtil.formatAsRuntime
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.ProvideSharedTransitionScope
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.feature.mobile.film.R
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

private val ThumbnailWidth = 120.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun EpisodeCard(
    episode: EpisodeWithProgress,
    onClick: () -> Unit,
    onLongClick: (EpisodeWithProgress) -> Unit,
    modifier: Modifier = Modifier,
    isDownloaded: Boolean = false,
) {
    val doHapticFeedback = getFeedbackOnLongPress()

    val description = episode.overview.trim().ifEmpty {
        stringResource(R.string.default_overview)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .padding(vertical = 4.dp, horizontal = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        doHapticFeedback()
                        onLongClick(episode)
                    },
                )
                .padding(vertical = 4.dp),
        ) {
            EpisodeThumbnail(
                episode = episode,
                isDownloaded = isDownloaded,
                modifier = Modifier
                    .align(Alignment.Top)
                    .width(getAdaptiveDp(ThumbnailWidth)),
            )

            EpisodeDetails(
                episode = episode,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )

            DownloadButton(
                onClick = {},
            )
        }

        ExpandableText(
            text = description,
            style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(
                size = 11.sp,
            ).let {
                it.copy(lineHeight = it.lineHeight * 0.85f)
            },
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            collapsedMaxLines = 3,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun EpisodeThumbnail(
    episode: EpisodeWithProgress,
    isDownloaded: Boolean,
    modifier: Modifier = Modifier,
) {
    var showPlaceholder by remember { mutableStateOf(true) }

    val surface = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            FilmCover.Backdrop(
                imagePath = episode.image,
                imageSize = "w300",
                title = episode.title,
                onSuccess = { showPlaceholder = false },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                    .clip(
                        MaterialTheme.shapes.small.copy(
                            bottomEnd = CornerSize(0),
                            bottomStart = CornerSize(0),
                        )
                    ),
            )

            if (showPlaceholder) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.movie_icon),
                    contentDescription = episode.title,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.6f to Color.Transparent,
                                1f to surface.copy(alpha = 0.7f),
                            ),
                        )
                    },
            )

            if (isDownloaded) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.download_done),
                    contentDescription = stringResource(R.string.downloaded),
                    dp = 14.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp),
                )
            }
        }

        episode.watchProgress?.let {
            ProgressBar(
                episodeProgress = it,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EpisodeDetails(
    episode: EpisodeWithProgress,
    modifier: Modifier = Modifier,
) {
    val title = episode.title.trim().ifEmpty {
        stringResource(LocaleR.string.untitled_episode, episode.number)
    }

    val duration = (episode.watchProgress?.duration?.takeIf { it > 0L }
        ?: episode.episode.runtime?.toLong()?.takeIf { it > 0L })
        ?.toInt()
        ?.formatAsRuntime()

    val episodeLabel = buildString {
        append(stringResource(R.string.episode_number_label, episode.episode.number))
        if (duration != null) {
            append(" • ")
            append(duration.asString())
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = episodeLabel,
            style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
                .copy(fontWeight = FontWeight.Bold)
                .asAdaptiveTextStyle(),
        )
    }
}

@Composable
private fun ProgressBar(
    episodeProgress: EpisodeProgress,
    modifier: Modifier = Modifier,
) {
    val progress = remember(episodeProgress) {
        when {
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
private fun ExpandableText(
    text: String,
    style: TextStyle,
    color: Color,
    collapsedMaxLines: Int,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) {
                    hasOverflow = result.hasVisualOverflow
                }
            },
        )

        if (hasOverflow || expanded) {
            Text(
                text = stringResource(
                    if (expanded) R.string.see_less else R.string.see_more,
                ),
                style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        expanded = !expanded
                    },
            )
        }
    }
}

@Composable
private fun DownloadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: Implement download functionality
    PlainTooltipBox(
        modifier = modifier,
        description = stringResource(LocaleR.string.download),
    ) {
        IconButton(onClick = onClick) {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.download),
                contentDescription = stringResource(LocaleR.string.download),
                dp = 22.dp,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
internal fun EpisodeCardPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        ) {
            Placeholder(
                modifier = Modifier
                    .width(getAdaptiveDp(ThumbnailWidth))
                    .aspectRatio(FilmCover.Backdrop.ratio),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Placeholder(
                    modifier = Modifier
                        .fillMaxWidth(0.3f)
                        .height(10.dp),
                )

                Placeholder(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp),
                )
            }

            Placeholder(
                modifier = Modifier.size(24.dp),
            )
        }

        Placeholder(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(top = 4.dp),
        )

        Placeholder(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(10.dp)
                .padding(top = 4.dp),
        )
    }
}

@Preview
@Composable
private fun EpisodeCardBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            ProvideSharedTransitionScope {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(getAdaptiveDp(300.dp)),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(
                        horizontal = DefaultScreenPaddingHorizontal,
                        vertical = 8.dp,
                    ),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 8.dp),
                ) {
                    items(6) {
                        val series = remember { DummyDataForPreview.getTvShow(id = "$it") }
                        val episode = remember {
                            series.seasons
                                .first()
                                .episodes
                                .first()
                                .copy(id = "$it", number = it + 1)
                        }

                        val episodeWithProgress = remember {
                            EpisodeWithProgress(
                                episode = episode,
                                watchProgress = EpisodeProgress(
                                    filmId = series.identifier,
                                    ownerId = -1,
                                    progress = 50000L,
                                    duration = 90000L,
                                    seasonNumber = episode.season,
                                    episodeNumber = episode.number,
                                    status = WatchStatus.WATCHING,
                                ),
                            )
                        }

                        EpisodeCard(
                            isDownloaded = true,
                            episode = episodeWithProgress,
                            onClick = {},
                            onLongClick = {},
                        )
                    }

                    items(3) {
                        EpisodeCardPlaceholder()
                    }
                }
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
