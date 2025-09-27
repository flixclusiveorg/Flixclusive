package com.flixclusive.feature.mobile.film.component

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.presentation.common.extensions.ifElse
import com.flixclusive.core.presentation.common.util.DummyDataForPreview
import com.flixclusive.core.presentation.mobile.AdaptiveTextStyle.asAdaptiveTextStyle
import com.flixclusive.core.presentation.mobile.components.AdaptiveIcon
import com.flixclusive.core.presentation.mobile.components.material3.PlainTooltipBox
import com.flixclusive.core.presentation.mobile.extensions.isCompact
import com.flixclusive.core.presentation.mobile.extensions.isMedium
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.film.R
import com.flixclusive.model.film.FilmMetadata
import com.flixclusive.model.film.FilmReleaseStatus
import com.flixclusive.core.drawables.R as UiCommonR
import com.flixclusive.core.strings.R as LocaleR

@Composable
internal fun HeaderButtons(
    metadata: FilmMetadata,
    watchProgress: WatchProgress?,
    isInLibrary: Boolean,
    onAddToLibrary: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloaded: Boolean = false, // TODO: Implement download functionality
    onToggleDownload: () -> Unit = {}, // TODO: Implement download functionality
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompactOrMedium = windowSizeClass.windowWidthSizeClass.isCompact
        || windowSizeClass.windowWidthSizeClass.isMedium

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .ifElse(
                    isCompactOrMedium,
                    ifTrueModifier = Modifier.weight(1f)
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (metadata.releaseStatus != FilmReleaseStatus.COMING_SOON) {
                PlayButton(
                    watchProgress = watchProgress,
                    onClick = onPlay,
                    modifier = Modifier,
                )
            } else {
                ComingSoonButton()
            }
        }

        ExtraButton(
            inactiveDrawable = if (isCompactOrMedium) R.drawable.add else UiCommonR.drawable.round_add_24,
            activeDrawable = if (isCompactOrMedium) R.drawable.added else UiCommonR.drawable.check,
            inactiveLabel = R.string.add,
            activeLabel = R.string.in_library,
            state = isInLibrary,
            onClick = onAddToLibrary,
        )

        if (metadata.releaseStatus != FilmReleaseStatus.COMING_SOON) {
            ExtraButton(
                inactiveLabel = LocaleR.string.download,
                activeLabel = R.string.downloaded,
                inactiveDrawable = UiCommonR.drawable.download,
                activeDrawable = UiCommonR.drawable.download_done,
                state = isDownloaded,
                onClick = onToggleDownload,
            )
        }
    }
}

@Composable
private fun PlayButton(
    watchProgress: WatchProgress?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val label = remember(watchProgress) { getPlayButtonLabel(context, watchProgress) }

    val transition = rememberInfiniteTransition(label = "Play Button Blob Animation")

    // Animate blobs smoothly
    val blob1X = transition.animateFloat(
        initialValue = 0f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Blob1X",
    )
    val blob1Y = transition.animateFloat(
        initialValue = 0f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Blob1Y",
    )

    val blob2X = transition.animateFloat(
        initialValue = 300f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Blob2X",
    )
    val blob2Y = transition.animateFloat(
        initialValue = 200f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Blob2Y",
    )

    val shape = MaterialTheme.shapes.small

    // Extract colors outside drawBehind
    val primaryBlobColor = MaterialTheme.colorScheme.primary
    val tertiaryBlobColor = MaterialTheme.colorScheme.tertiary

    // Gradient background for the "liquid fill"
    val baseGradient = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f),
        ),
    )

    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onPrimary,
    ) {
        Box(
            modifier =
                modifier
                    .minimumInteractiveComponentSize()
                    .widthIn(min = 125.dp)
                    .clip(shape)
                    .drawBehind {
                        val width = size.width
                        val height = size.height

                        // First draw the base gradient fill
                        drawRect(
                            brush = baseGradient,
                            size = size,
                        )

                        // Compute blob positions
                        val pos1 = Offset(
                            x = (blob1X.value / 300f) * width,
                            y = (blob1Y.value / 200f) * height,
                        )
                        val pos2 = Offset(
                            x = (blob2X.value / 300f) * width,
                            y = (blob2Y.value / 200f) * height,
                        )

                        // Draw blobs on top of gradient
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(primaryBlobColor, Color.Transparent),
                                center = pos1,
                                radius = width / 1.4f,
                            ),
                            radius = width / 1.4f,
                            center = pos1,
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(tertiaryBlobColor, Color.Transparent),
                                center = pos2,
                                radius = width / 1.6f,
                            ),
                            radius = width / 1.6f,
                            center = pos2,
                        )
                    }
                    .focusable()
                    .clickable { onClick() },
            propagateMinConstraints = true
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .padding(
                        vertical = 10.dp,
                        horizontal = 16.dp
                    )
                    .align(Alignment.Center),
            ) {
                AdaptiveIcon(
                    painter = painterResource(UiCommonR.drawable.play),
                    contentDescription = label,
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
                )
            }
        }
    }
}

@Composable
private fun ComingSoonButton() {
    OutlinedButton(
        onClick = {},
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(0.08F),
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(0.3F),
        ),
        enabled = false,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AdaptiveIcon(
                painter = painterResource(UiCommonR.drawable.time_circle_outlined),
                contentDescription = stringResource(LocaleR.string.coming_soon),
            )

            Text(
                text = stringResource(LocaleR.string.coming_soon),
                style = MaterialTheme.typography.labelLarge.asAdaptiveTextStyle(),
            )
        }
    }
}

@Composable
private fun ExtraButton(
    inactiveLabel: Int,
    activeLabel: Int,
    inactiveDrawable: Int,
    activeDrawable: Int,
    state: Boolean,
    onClick: () -> Unit,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isCompactOrMedium = windowSizeClass.windowWidthSizeClass.isCompact
        || windowSizeClass.windowWidthSizeClass.isMedium

    val label = if (state) {
        stringResource(activeLabel)
    } else {
        stringResource(inactiveLabel)
    }

    @Composable
    fun LabelIconContent() {
        AdaptiveIcon(
            painter = if (state) {
                painterResource(activeDrawable)
            } else {
                painterResource(inactiveDrawable)
            },
            contentDescription = label,
            tint = if (state) {
                LocalContentColor.current
            } else {
                MaterialTheme.colorScheme.onSurface.copy(0.6F)
            },
            dp = if (isCompactOrMedium) null else 18.dp,
        )

        if(isCompactOrMedium) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.asAdaptiveTextStyle(),
                color = MaterialTheme.colorScheme.onSurface.copy(0.6F),
            )
        }
    }

    PlainTooltipBox(description = label) {
        // Use full OutlinedButton on larger screens, icon-only button on compact screens
        if (!isCompactOrMedium) {
            val colors = if (state) {
                ButtonDefaults.buttonColors()
            } else {
                ButtonDefaults.outlinedButtonColors()
            }

            val border = if (state) {
                null
            } else {
                BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4F)
                )
            }

            Button(
                onClick = onClick,
                shape = MaterialTheme.shapes.small,
                colors = colors,
                border = border,
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 15.dp)
            ) {
                LabelIconContent()
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .sizeIn(50.dp)
                    .clip(MaterialTheme.shapes.small)
                    .focusable()
                    .clickable { onClick() }
                    .padding(3.dp),
            ) {
                LabelIconContent()
            }
        }

    }
}

private fun getPlayButtonLabel(
    context: Context,
    watchProgress: WatchProgress?,
): String {
    return if (watchProgress?.isFinished == true) {
        context.getString(LocaleR.string.watch_again)
    } else if (watchProgress?.isWatching == true && watchProgress is MovieProgress) {
        context.getString(LocaleR.string.continue_watching)
    } else if (watchProgress?.isWatching == true && watchProgress is EpisodeProgress) {
        val season = watchProgress.seasonNumber
        val episode = watchProgress.episodeNumber
        context.getString(LocaleR.string.continue_watching_tv_show, season, episode)
    } else {
        context.getString(LocaleR.string.play)
    }
}

@Preview
@Composable
private fun HeaderButtonsPreview() {
    val metadata = remember { DummyDataForPreview.getMovie() }
    var isInLibrary by remember { mutableStateOf(false) }

    FlixclusiveTheme {
        Surface {
            Column {
                HeaderButtons(
                    metadata = metadata,
                    watchProgress = remember {
                        MovieProgress(
                            filmId = metadata.identifier,
                            ownerId = 0,
                            progress = 500L,
                            status = WatchStatus.WATCHING,
                            duration = 6000L,
                        )
                    },
                    isInLibrary = isInLibrary,
                    onAddToLibrary = { isInLibrary = !isInLibrary },
                    onPlay = {},
                )

                HeaderButtons(
                    metadata = metadata.copy(
                        releaseDate = "2099-01-01",
                    ),
                    watchProgress = remember {
                        MovieProgress(
                            filmId = metadata.identifier,
                            ownerId = 0,
                            progress = 500L,
                            status = WatchStatus.WATCHING,
                            duration = 6000L,
                        )
                    },
                    isInLibrary = isInLibrary,
                    onAddToLibrary = { isInLibrary = !isInLibrary },
                    onPlay = {},
                )
            }
        }
    }
}
