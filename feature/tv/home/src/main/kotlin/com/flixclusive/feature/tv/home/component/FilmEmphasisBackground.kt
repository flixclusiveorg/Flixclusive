package com.flixclusive.feature.tv.home.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.model.film.Film

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun FilmEmphasisBackground(
    film: Film?,
    backgroundHeight: Dp,
    gradientColor: Color = MaterialTheme.colorScheme.surface,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
) {
    AnimatedContent(
        targetState = film,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = enter,
                initialContentExit = exit
            )
        },
        modifier = Modifier
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                gradientColor,
                            ),
                            endY = size.width.times(0.6f)
                        )
                    )
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                gradientColor,
                                Color.Transparent
                            ),
                            start = Offset(
                                size.width.times(0.2f),
                                size.height.times(0.5f)
                            ),
                            end = Offset(
                                size.width.times(0.9f),
                                0f
                            )
                        )
                    )
                }
            },
        label = ""
    ) {
        val backdropImage = LocalContext.current.buildImageUrl(
            imagePath = it?.backdropImage,
            imageSize = "w1920_and_h600_multi_faces"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AsyncImage(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .height(backgroundHeight),
                model = backdropImage,
                imageLoader = LocalContext.current.imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
}