package com.flixclusive.feature.mobile.film.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.presentation.common.components.FilmCover
import com.flixclusive.core.presentation.common.extensions.ifElse
import com.flixclusive.core.presentation.common.extensions.noIndicationClickable
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.common.util.FilmFormatterUtil.formatAsRating
import com.flixclusive.core.presentation.common.util.FilmFormatterUtil.formatAsRuntime
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.ProvideAnimatedVisibilityScope
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.ProvideSharedTransitionScope
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.getLocalAnimatedVisibilityScope
import com.flixclusive.core.presentation.common.util.SharedTransitionUtil.getLocalSharedTransitionScope
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.Placeholder
import com.flixclusive.core.presentation.mobile.components.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.core.presentation.mobile.util.AdaptiveSizeUtil.getAdaptiveDp
import com.flixclusive.core.presentation.mobile.util.MobileUiUtil.DefaultScreenPaddingHorizontal
import com.flixclusive.core.presentation.mobile.util.getFeedbackOnLongPress
import com.flixclusive.domain.provider.model.EpisodeWithProgress
import com.flixclusive.feature.mobile.film.R
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@OptIn(ExperimentalSharedTransitionApi::class)
private val boundsTransition = BoundsTransform { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy) }

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun EpisodeCard(
    episode: EpisodeWithProgress,
    visible: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloaded: Boolean = false, // TODO: Implement download state
) {
    val doHapticFeedback = getFeedbackOnLongPress()

    val sharedTransitionScope = getLocalSharedTransitionScope()

    var width by remember { mutableStateOf<Dp?>(null) }
    val density = LocalDensity.current

    Box(
        modifier = modifier.onSizeChanged {
            width = with(density) { it.width.toDp() }
        },
    ) {
        // Whenever the item disappears due to the shared transition, we want to keep its space
        // so that the other items don't shift around
        width?.let {
            Spacer(modifier = Modifier.width(it))
        }

        with(sharedTransitionScope) {
            AnimatedVisibility(visible) {
                ProvideAnimatedVisibilityScope {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.1f)
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(
                                    key = "${episode.number}_${episode.season}_bounds",
                                ),
                                animatedVisibilityScope = this,
                                boundsTransform = boundsTransition,
                            )
                            .clip(MaterialTheme.shapes.small)
                            .combinedClickable(
                                onClick = onClick,
                                onLongClick = {
                                    doHapticFeedback()
                                    onLongClick()
                                },
                            ).padding(4.dp),
                    ) {
                        EpisodePreview(
                            episode = episode,
                            isDownloaded = isDownloaded,
                        )

                        EpisodeDetails(
                            episode = episode,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SharedTransitionScope.EpisodeDetailedCard(
    episodeWithProgress: EpisodeWithProgress?,
    isDownloaded: Boolean,
    toggleOnLibrary: (EpisodeWithProgress) -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        modifier = modifier,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        targetState = episodeWithProgress,
    ) { episode ->
        ProvideAnimatedVisibilityScope {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (episode == null) return@ProvideAnimatedVisibilityScope

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.20f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismissRequest,
                        ),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .width(getAdaptiveDp(300.dp, 50.dp))
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "${episode.number}_${episode.season}_bounds",
                            ),
                            animatedVisibilityScope = this@AnimatedContent,
                            boundsTransform = boundsTransition,
                        )
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                        .noIndicationClickable {},
                ) {
                    EpisodePreview(
                        episode = episode,
                        isDownloaded = false, // Downloaded icon is not needed in detailed view
                        isDetailed = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    EpisodeDetails(
                        episode = episode,
                        isDetailed = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp),
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                        thickness = 0.5.dp
                    )

                    RowActions(
                        episodeWithProgress = episode,
                        isDownloaded = isDownloaded,
                        onPlay = onPlay,
                        onDownload = onDownload,
                        toggleOnLibrary = toggleOnLibrary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 5.dp)
                            .padding(bottom = 10.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun EpisodePreview(
    episode: EpisodeWithProgress,
    isDownloaded: Boolean,
    modifier: Modifier = Modifier,
    isDetailed: Boolean = false,
) {
    val imageWidthModifier = Modifier.fillMaxWidth()
    var showPlaceholder by remember { mutableStateOf(true) }

    val surface = MaterialTheme.colorScheme.surface

    val sharedTransitionScope = getLocalSharedTransitionScope()
    val animatedVisibilityScope = getLocalAnimatedVisibilityScope()

    with(sharedTransitionScope) {
        Column(
            modifier = modifier,
        ) {
            Box(contentAlignment = Alignment.Center) {
                FilmCover.Backdrop(
                    imagePath = episode.image,
                    imageSize = "w300",
                    title = episode.title,
                    onSuccess = { showPlaceholder = false },
                    modifier = imageWidthModifier
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                        .sharedElement(
                            state = rememberSharedContentState(key = "${episode.number}_image"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = boundsTransition,
                        )
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
                        modifier = Modifier.sharedElement(
                            state = rememberSharedContentState(key = "${episode.number}_image_placeholder"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = boundsTransition,
                        ),
                    )
                }

                // Scrim
                Box(
                    modifier = Modifier
                        .sharedElement(
                            state = rememberSharedContentState(key = "${episode.number}_image_scrim"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = boundsTransition,
                        ).matchParentSize()
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

                Duration(
                    episode = episode,
                    isDetailed = isDetailed,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "${episode.number}_episode_duration"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = boundsTransition,
                        ),
                )

                if (isDownloaded) {
                    AdaptiveIcon(
                        painter = painterResource(UiCommonR.drawable.download_done),
                        contentDescription = stringResource(R.string.downloaded),
                        dp = 14.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .sharedElement(
                                state = rememberSharedContentState(key = "${episode.number}_downloaded"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = boundsTransition,
                            )
                    )
                }
            }

            ProgressBar(
                episodeProgress = episode.watchProgress,
                modifier = imageWidthModifier
                    .sharedElement(
                        state = rememberSharedContentState(key = "${episode.number}_episode_progress"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = boundsTransition,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun EpisodeDetails(
    episode: EpisodeWithProgress,
    modifier: Modifier = Modifier,
    isDetailed: Boolean = false,
) {
    val sharedTransitionScope = getLocalSharedTransitionScope()
    val animatedVisibilityScope = getLocalAnimatedVisibilityScope()

    val title = episode.title.trim().ifEmpty {
        stringResource(LocaleR.string.untitled_episode, episode.number)
    }

    val extraInformation = buildAnnotatedString {
        if (episode.episode.rating != null) {
            append(
                episode.episode.rating!!
                    .formatAsRating()
                    .asString(),
            )
            append(" • ")
        }

        if (episode.episode.releaseDate.isNotEmpty()) {
            append(episode.episode.releaseDate)
        }
    }

    val description = episode.overview.trim().ifEmpty {
        stringResource(R.string.default_overview)
    }

    val titleStyle = if (isDetailed) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.labelMedium
    }

    with(sharedTransitionScope) {
        Column(
            modifier = modifier
                .ifElse(
                    condition = isDetailed,
                    Modifier.padding(horizontal = 10.dp)
                )
        ) {
            Text(
                text = "${episode.episode.number}. $title",
                style = titleStyle
                    .copy(fontWeight = FontWeight.Black)
                    .asAdaptiveTextStyle(),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(bottom = 3.dp)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "${episode.number}_episode_title"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = boundsTransition,
                    ),
            )

            if (extraInformation.isNotEmpty() && isDetailed) {
                Text(
                    text = extraInformation,
                    style = MaterialTheme.typography.labelMedium.asAdaptiveTextStyle(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Thin,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.asAdaptiveTextStyle(
                    size = if (isDetailed) 14.sp else 10.sp,
                ).let {
                    it.copy(lineHeight = it.lineHeight * 0.85f)
                },
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = if (isDetailed) Int.MAX_VALUE else 2,
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "${episode.number}_episode_description"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = boundsTransition,
                    ),
            )
        }
    }
}

@Composable
private fun Duration(
    episode: EpisodeWithProgress,
    modifier: Modifier = Modifier,
    isDetailed: Boolean = false,
) {
    val lessEmphasisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val duration = (episode.watchProgress?.duration ?: episode.episode.runtime?.toLong())
        .takeIf { it != null && it > 0L }
        ?.toInt()
        ?.formatAsRuntime()

    if (duration != null) {
        Row(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.extraSmall,
                ).padding(horizontal = 3.dp),
        ) {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.time_circle_outlined),
                contentDescription = duration.asString(),
                tint = lessEmphasisColor,
                dp = if (isDetailed) 10.dp else 8.dp,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 3.dp),
            )

            Text(
                text = duration.asString(),
                style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(
                    size = if (isDetailed) 12.sp else 8.sp
                ),
                color = lessEmphasisColor,
            )
        }
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun RowActions(
    episodeWithProgress: EpisodeWithProgress,
    isDownloaded: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    toggleOnLibrary: (EpisodeWithProgress) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sharedTransitionScope = getLocalSharedTransitionScope()
    val animatedVisibilityScope = getLocalAnimatedVisibilityScope()

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        ActionButton(
            state = true,
            activeDrawable = UiCommonR.drawable.play,
            inactiveDrawable = UiCommonR.drawable.play,
            inactiveLabel = LocaleR.string.play,
            activeLabel = LocaleR.string.play,
            onClick = onPlay,
        )

        with(sharedTransitionScope) {
            ActionButton(
                state = isDownloaded,
                activeDrawable = UiCommonR.drawable.download_done,
                inactiveDrawable = UiCommonR.drawable.download,
                inactiveLabel = LocaleR.string.download,
                activeLabel = R.string.downloaded,
                onClick = onDownload,
                iconModifier = Modifier
                    .sharedElement(
                        state = rememberSharedContentState(key = "${episodeWithProgress.number}_downloaded"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = boundsTransition,
                    )
            )
        }

        ActionButton(
            state = episodeWithProgress.watchProgress != null,
            activeDrawable = UiCommonR.drawable.round_close_24,
            inactiveDrawable = UiCommonR.drawable.check,
            activeLabel = LocaleR.string.unwatch,
            inactiveLabel = LocaleR.string.watched,
            onClick = { toggleOnLibrary(episodeWithProgress) }
        )
    }
}

@Composable
private fun RowScope.ActionButton(
    inactiveLabel: Int,
    activeLabel: Int,
    inactiveDrawable: Int,
    activeDrawable: Int,
    state: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    val label = if (state) {
        stringResource(activeLabel)
    } else {
        stringResource(inactiveLabel)
    }

    PlainTooltipBox(
        modifier = modifier,
        description = label
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(0.3f)
                .minimumInteractiveComponentSize()
                .clip(MaterialTheme.shapes.small)
                .focusable()
                .clickable { onClick() }
                .padding(3.dp),
        ) {
            AnimatedContent(state) {
                AdaptiveIcon(
                    modifier = iconModifier,
                    painter = if (it) {
                        painterResource(activeDrawable)
                    } else {
                        painterResource(inactiveDrawable)
                    },
                    contentDescription = label,
                    dp = 24.dp,
                    tint = if (it) {
                        LocalContentColor.current
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(0.6F)
                    },
                )
            }

            AnimatedContent(label) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6F),
                )
            }
        }
    }
}

@Composable
internal fun EpisodeCardPlaceholder() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .padding(4.dp),
    ) {
        Placeholder(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(FilmCover.Backdrop.ratio),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            Placeholder(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(13.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Placeholder(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                )

                Placeholder(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(10.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
private fun EpisodeCardBasePreview() {
    var longClickedEpisode by remember { mutableStateOf<EpisodeWithProgress?>(null) }

    FlixclusiveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            ProvideSharedTransitionScope {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(getAdaptiveDp(135.dp)),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    contentPadding = PaddingValues(
                        horizontal = DefaultScreenPaddingHorizontal,
                        vertical = 8.dp,
                    ),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 8.dp),
                ) {
                    items(3) {
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
                            visible = episodeWithProgress != longClickedEpisode,
                            onClick = {},
                            onLongClick = { longClickedEpisode = episodeWithProgress },
                        )
                    }

                    items(3) {
                        EpisodeCardPlaceholder()
                    }
                }

                EpisodeDetailedCard(
                    episodeWithProgress = longClickedEpisode,
                    isDownloaded = true,
                    toggleOnLibrary = {},
                    onPlay = {},
                    onDownload = {},
                    onDismissRequest = { longClickedEpisode = null },
                )
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
