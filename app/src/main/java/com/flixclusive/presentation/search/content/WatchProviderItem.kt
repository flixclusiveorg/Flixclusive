package com.flixclusive.presentation.search.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flixclusive.domain.model.tmdb.WatchProvider
import com.flixclusive.presentation.common.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.common.composables.placeholderEffect

@Composable
fun WatchProviderItem(
    modifier: Modifier = Modifier,
    item: WatchProvider,
    onWatchProviderClick: () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = RoundedCornerShape(5.dp)
                clip = true
            }
            .drawBehind {
                drawRect(Color.LightGray)
            }
            .clickable {
                onWatchProviderClick()
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = context.buildImageUrl(
                imagePath = item.posterPath,
                imageSize = "original"
            ),
            contentDescription = UiText.StringResource(item.labelId).asString(),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(85.dp)
        )
    }
}

@Composable
fun WatchHistoryItemPlaceholder() {
    Spacer(
        modifier = Modifier
            .height(88.dp)
            .width(190.dp)
            .padding(10.dp)
            .placeholderEffect()
    )
}