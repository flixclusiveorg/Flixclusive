package com.flixclusive.feature.mobile.library.manage.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.FilmCover
import com.flixclusive.core.ui.common.R
import com.flixclusive.core.ui.common.adaptive.AdaptiveIcon
import com.flixclusive.core.ui.common.util.CoilUtil.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.ui.common.util.CoilUtil.buildImageUrl
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.boxShadow
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.common.util.placeholderEffect
import com.flixclusive.feature.mobile.library.manage.PreviewPoster
import com.flixclusive.feature.mobile.library.manage.PreviewPoster.Companion.toPreviewPoster
import com.flixclusive.model.database.DBFilm
import com.flixclusive.core.locale.R as LocaleR
import com.flixclusive.core.ui.common.R as UiCommonR

private val MaxMainCardWidth = 90.dp
internal val StackedCardsMaxWidth = 150.dp

@Composable
internal fun StackedPosters(
    previews: List<PreviewPoster>,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val maxMainCardWidth = getAdaptiveDp(MaxMainCardWidth)
    val maxBackgroundCardWidth = maxMainCardWidth * 0.8f
    val positionX = maxBackgroundCardWidth * 0.25f

    val backgroundCardWidthModifier =
        Modifier
            .width(maxBackgroundCardWidth)
            .graphicsLayer { shadowElevation = 24f }

    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                drawRect(surface.copy(0.1f))
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Right card
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .matchParentSize()
                    .offset { IntOffset((positionX).roundToPx(), 0) }
                    .graphicsLayer { rotationZ = 5f },
        ) {
            PreviewCard(
                preview = previews.getOrNull(2),
                modifier = backgroundCardWidthModifier,
            )
        }

        // Left card
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .matchParentSize()
                    .offset { IntOffset(-(positionX).roundToPx(), 0) }
                    .graphicsLayer { rotationZ = -5f },
        ) {
            PreviewCard(
                preview = previews.getOrNull(1),
                modifier = backgroundCardWidthModifier,
            )
        }

        // Main front card
        PreviewCard(
            preview = previews.getOrNull(0),
            modifier =
                Modifier
                    .width(maxMainCardWidth)
                    .boxShadow(
                        color = MaterialTheme.colorScheme.surface.copy(0.8f),
                        blurRadius = 6.dp,
                        spreadRadius = 2.dp
                    ),
        )
    }
}

@Composable
private fun PreviewCard(
    preview: PreviewPoster?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isHidingPlaceholder by remember(preview) { mutableStateOf(preview != null) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (preview != null) {
            val painter =
                remember(preview.posterPath) {
                    context.buildImageUrl(
                        imagePath = preview.posterPath,
                        imageSize = "w300",
                    )
                }

            AsyncImage(
                model = painter,
                imageLoader = LocalContext.current.imageLoader,
                contentScale = ContentScale.FillBounds,
                contentDescription = stringResource(id = LocaleR.string.film_item_content_description),
                onSuccess = { isHidingPlaceholder = true },
                modifier =
                    Modifier
                        .aspectRatio(FilmCover.Poster.ratio)
                        .clip(MaterialTheme.shapes.extraSmall),
            )
        }

        AnimatedVisibility(
            visible = !isHidingPlaceholder,
            enter = fadeIn(),
            exit = fadeOut(),
            label = "PlaceHolder",
        ) {
            PreviewPlaceholder(
                title = preview?.title,
                modifier = Modifier.aspectRatio(FilmCover.Poster.ratio),
            )
        }
    }
}

@Composable
private fun PreviewPlaceholder(
    title: String?,
    modifier: Modifier = Modifier,
) {
    val contentColor = LocalContentColor.current.onMediumEmphasis()

    val (iconWeight, iconAlignment) =
        remember {
            if (title != null) {
                0.4F to Alignment.BottomCenter
            } else {
                1F to Alignment.Center
            }
        }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .height(200.dp)
                    .fillMaxWidth(),
        ) {
            Spacer(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .placeholderEffect(),
            )

            if (title != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        contentAlignment = iconAlignment,
                        modifier = Modifier.weight(iconWeight),
                    ) {
                        AdaptiveIcon(
                            painter = painterResource(id = R.drawable.movie_icon),
                            contentDescription = stringResource(id = LocaleR.string.film_item_content_description),
                            tint = contentColor,
                            dp = 25.dp,
                            increaseBy = 3.dp
                        )
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier =
                            Modifier
                                .weight(0.6F),
                    ) {
                        Text(
                            text = title,
                            style = getAdaptiveTextStyle(
                                size = 10.sp,
                                increaseBy = 2.sp
                            ),
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier
                                    .padding(8.dp),
                        )
                    }
                }
            } else {
                AdaptiveIcon(
                    painter = painterResource(id = UiCommonR.drawable.round_add_24),
                    contentDescription = stringResource(LocaleR.string.add_to_list),
                    tint = LocalContentColor.current.onMediumEmphasis(),
                    dp = 25.dp,
                    increaseBy = 3.dp
                )
            }
        }
    }
}

@Preview
@Composable
private fun StackedPostersBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            ProvideAsyncImagePreviewHandler(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(getAdaptiveDp(StackedCardsMaxWidth)),
                    horizontalArrangement = Arrangement.spacedBy(25.dp),
                    verticalArrangement = Arrangement.spacedBy(50.dp),
                ) {
                    items(20) {
                        StackedPosters(
                            previews =
                                List(3) {
                                    DBFilm(title = "Film #$it").toPreviewPoster()
                                },
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun StackedPostersCompactLandscapePreview() {
    StackedPostersBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun StackedPostersMediumPortraitPreview() {
    StackedPostersBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun StackedPostersMediumLandscapePreview() {
    StackedPostersBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun StackedPostersExtendedPortraitPreview() {
    StackedPostersBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun StackedPostersExtendedLandscapePreview() {
    StackedPostersBasePreview()
}
