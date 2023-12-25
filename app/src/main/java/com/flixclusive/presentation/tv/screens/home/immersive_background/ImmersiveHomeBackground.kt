package com.flixclusive.presentation.tv.screens.home.immersive_background

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.tv.common.composables.FilmTvOverview
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.LabelStartPadding
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.drawScrimOnBackground
import com.flixclusive.presentation.utils.ImageRequestCreator.buildImageUrl

@Composable
fun ImmersiveHomeBackground(
    modifier: Modifier = Modifier,
    headerItem: Film?,
    backgroundHeight: Dp
) {
    val context = LocalContext.current
    val gradientColor = MaterialTheme.colorScheme.surface
    val enterAnimation = fadeIn(initialAlpha = 0.6F)
    val exitAnimation = fadeOut(targetAlpha = 0.6F)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(backgroundHeight)
            .background(gradientColor)
    ) {
        AnimatedContent(
            targetState = headerItem,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = enterAnimation,
                    initialContentExit = exitAnimation
                )
            },
            modifier = Modifier.drawScrimOnBackground(),
            label = ""
        ) {
            val backdropImage = context.buildImageUrl(
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

        AnimatedContent(
            targetState = headerItem,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = enterAnimation,
                    initialContentExit = exitAnimation
                )
            },
            label = ""
        ) {
            it?.let { film ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(LabelStartPadding.getPaddingValues())
                        .padding(top = 20.dp)
                ) {
                    FilmTvOverview(film = film)
                }
            }
        }
    }
}