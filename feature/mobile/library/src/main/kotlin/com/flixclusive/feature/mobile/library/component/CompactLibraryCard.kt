package com.flixclusive.feature.mobile.library.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.flixclusive.core.theme.FlixclusiveTheme
import com.flixclusive.core.ui.common.FilmCover
import com.flixclusive.core.ui.common.util.CoilUtil.ProvideAsyncImagePreviewHandler
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveStylesUtil.getAdaptiveTextStyle
import com.flixclusive.core.ui.common.util.adaptive.AdaptiveUiUtil.getAdaptiveDp
import com.flixclusive.core.ui.common.util.adaptive.TextStyleMode
import com.flixclusive.core.ui.common.util.adaptive.TypographyStyle
import com.flixclusive.core.ui.mobile.component.film.FilmCardPlaceholder
import com.flixclusive.model.database.DBFilm
import com.flixclusive.model.film.Film

private val MaxMainCardWidth = 110.dp
internal val StackedCardsMaxWidth = 150.dp

@Composable
internal fun CompactLibraryCard(
    name: String,
    films: List<Film>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxMainCardWidth = getAdaptiveDp(MaxMainCardWidth)
    val maxBackgroundCardWidth = maxMainCardWidth * 0.8f
    val positionX = maxBackgroundCardWidth * 0.25f

    val overlayColor = Color.Black
    val backgroundCardWidthModifier = Modifier.width(maxBackgroundCardWidth)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        // Right card
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .matchParentSize()
                .offset { IntOffset((positionX).roundToPx(), 0) }
                .graphicsLayer { rotationZ = 5f }
        ) {
            FilmCard(
                title = films[2].title,
                posterImage = films[2].posterImage,
                onClick = onClick,
                modifier = backgroundCardWidthModifier,
            )
        }

        // Left card
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .matchParentSize()
                .offset { IntOffset(-(positionX).roundToPx(), 0) }
                .graphicsLayer { rotationZ = -5f }
        ) {
            FilmCard(
                title = films[1].title,
                posterImage = films[1].posterImage,
                onClick = onClick,
                modifier = backgroundCardWidthModifier,
            )
        }

        // Main front card
        FilmCard(
            title = films[0].title,
            posterImage = films[0].posterImage,
            hasPlaceholder = true,
            onClick = onClick,
            modifier = Modifier
                .width(maxMainCardWidth)
                .graphicsLayer { shadowElevation = 50f },
        )

        Box(
            modifier = Modifier
                .width(getAdaptiveDp(StackedCardsMaxWidth))
                .aspectRatio(1f)
                .background(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.9f to overlayColor
                    ),
                    shape = MaterialTheme.shapes.small
                )
        )

        Box(
            modifier = Modifier.width(maxMainCardWidth * 1.2f),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = name,
                style = getAdaptiveTextStyle(
                    mode = TextStyleMode.Emphasized,
                    style = TypographyStyle.Label
                ),
                modifier = Modifier
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun FilmCard(
    title: String,
    posterImage: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasPlaceholder: Boolean = false,
) {
    var isHidingPlaceholder by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        FilmCover.Poster(
            imagePath = posterImage,
            imageSize = "w300",
            showPlaceholder = false,
            onSuccess = { isHidingPlaceholder = true },
            onClick = onClick,
        )

        if (hasPlaceholder) {
            AnimatedVisibility(
                visible = !isHidingPlaceholder,
                enter = fadeIn(),
                exit = fadeOut(),
                label = "PlaceHolder"
            ) {
                FilmCardPlaceholder(
                    isShowingTitle = true,
                    title = title,
                    modifier = Modifier
                        .aspectRatio(FilmCover.Poster.ratio)
                )
            }
        }
    }
}

@Preview
@Composable
private fun CompactLibraryCardBasePreview() {
    FlixclusiveTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            ProvideAsyncImagePreviewHandler(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(getAdaptiveDp(StackedCardsMaxWidth)),
                    horizontalArrangement = Arrangement.spacedBy(25.dp),
                    verticalArrangement = Arrangement.spacedBy(50.dp),
                ) {
                    items(20) {
                        CompactLibraryCard(
                            name = "Library #$it",
                            films = List(3) {
                                DBFilm(title = "Film #$it")
                            },
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun CompactLibraryCardCompactLandscapePreview() {
    CompactLibraryCardBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=portrait")
@Composable
private fun CompactLibraryCardMediumPortraitPreview() {
    CompactLibraryCardBasePreview()
}

@Preview(device = "spec:parent=medium_tablet,orientation=landscape")
@Composable
private fun CompactLibraryCardMediumLandscapePreview() {
    CompactLibraryCardBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=portrait")
@Composable
private fun CompactLibraryCardExtendedPortraitPreview() {
    CompactLibraryCardBasePreview()
}

@Preview(device = "spec:width=1920dp,height=1080dp,dpi=160,orientation=landscape")
@Composable
private fun CompactLibraryCardExtendedLandscapePreview() {
    CompactLibraryCardBasePreview()
}
