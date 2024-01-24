package com.flixclusive.feature.tv.home.component

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
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.core.ui.tv.FilmOverview
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.drawScrimOnBackground
import com.flixclusive.model.tmdb.Film

@OptIn(ExperimentalTvMaterial3Api::class)
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
                    FilmOverview(film = film)
                }
            }
        }
    }
}