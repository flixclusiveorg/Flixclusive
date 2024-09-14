package com.flixclusive.feature.tv.home.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.core.ui.tv.component.FilmOverview
import com.flixclusive.core.ui.tv.util.LabelStartPadding
import com.flixclusive.core.ui.tv.util.drawScrimOnForeground
import com.flixclusive.model.film.Film

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ImmersiveHomeBackground(
    modifier: Modifier = Modifier,
    headerItem: Film?
) {
    val context = LocalContext.current
    val enterAnimation = fadeIn(initialAlpha = 0.6F)
    val exitAnimation = fadeOut(targetAlpha = 0.6F)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AnimatedContent(
            targetState = headerItem,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = enterAnimation,
                    initialContentExit = exitAnimation
                )
            },
            modifier = Modifier.drawScrimOnForeground(),
            label = ""
        ) {
            val backdropImage = context.buildImageUrl(
                imagePath = it?.backdropImage,
                imageSize = "w1280"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                AsyncImage(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight(),
                    model = backdropImage,
                    imageLoader = LocalContext.current.imageLoader,
                    contentDescription = null
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
                    FilmOverview(
                        film = film,
                        watchHistoryItem = null
                    ) // TODO: Add watch history item here.
                }
            }
        }
    }
}