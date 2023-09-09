package com.flixclusive.presentation.mobile.screens.search.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flixclusive.presentation.theme.lightGrayElevated
import com.flixclusive.presentation.tv.utils.ModifierTvUtils.ifElse
import com.flixclusive.presentation.utils.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.utils.ModifierUtils.placeholderEffect


@Composable
fun SearchItemCard(
    modifier: Modifier = Modifier,
    posterPath: String?,
    label: String? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val poster = remember(posterPath) {
        context.buildImageUrl(
            imagePath = posterPath,
            imageSize = if (label != null) "w533_and_h300_bestv2" else "w500_filter(negate,000,666)"
        )
    }

    Surface(
        tonalElevation = 3.dp,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() },
        ) {
            AsyncImage(
                model = poster,
                contentDescription = label,
                contentScale = if (label == null) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .ifElse(
                        condition = label == null,
                        ifTrueModifier = Modifier.padding(10.dp)
                    )
            )

            if (label != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black
                                    ),
                                    startY = 0F,
                                    endY = size.height.times(0.9F)
                                )
                            )
                        },
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(
                            start = 8.dp,
                            bottom = 8.dp,
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SearchItemCardPlaceholderWithText() {
    Box(
        modifier = Modifier
            .height(130.dp)
            .width(190.dp)
            .padding(10.dp)
            .placeholderEffect(shape = MaterialTheme.shapes.medium)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 8.dp,
                    bottom = 8.dp,
                ),
            contentAlignment = Alignment.BottomStart
        ) {
            Spacer(
                modifier = Modifier
                    .height(14.dp)
                    .width(60.dp)
                    .graphicsLayer {
                        shape = RoundedCornerShape(100)
                        clip = true
                    }
                    .drawBehind {
                        drawRect(lightGrayElevated)
                    }
            )
        }
    }
}