@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.flixclusive.core.ui.tv.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardLayoutDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.StandardCardLayout
import com.flixclusive.core.ui.common.FilmCover
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.util.Padding
import com.flixclusive.model.film.Film

val FilmCardWidth = 100.dp
val FilmCardHeight = 150.dp
val FilmCardShape = ShapeDefaults.ExtraSmall
val FilmPadding = Padding(3.dp)

@Composable
fun FilmCard(
    modifier: Modifier = Modifier,
    film: Film,
    onClick: (Film) -> Unit,
    contentPadding: PaddingValues = FilmPadding.getPaddingValues(),
    filmCardHeight: Dp = FilmCardHeight,
    filmCardWidth: Dp = FilmCardWidth,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")

    val borderDp by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = 3.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    StandardCardLayout(
        modifier = modifier
            .padding(contentPadding),
        imageCard = {
            CardLayoutDefaults.ImageCard(
                onClick = { onClick(film) },
                interactionSource = it,
                shape = CardDefaults.shape(FilmCardShape),
                glow = CardDefaults.glow(
                    focusedGlow = Glow(
                        elevationColor = MaterialTheme.colorScheme.primary.onMediumEmphasis(),
                        elevation = 15.dp
                    ),
                    pressedGlow = Glow(
                        elevationColor = MaterialTheme.colorScheme.primary.onMediumEmphasis(),
                        elevation = 40.dp
                    ),
                ),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = borderDp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = FilmCardShape
                    )
                ),
                scale = CardDefaults.scale(focusedScale = 1F),
            ) {
                FilmCover.Poster(
                    imagePath = film.posterImage,
                    imageSize = "w300",
                    showPlaceholder = false,
                    onClick = { onClick(film) },
                    modifier = Modifier
                        .size(filmCardWidth, filmCardHeight)
                )
            }
        },
        title = {}
    )
}