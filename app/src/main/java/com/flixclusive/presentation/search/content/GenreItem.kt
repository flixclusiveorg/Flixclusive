package com.flixclusive.presentation.search.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.common.composables.placeholderEffect
import com.flixclusive.ui.theme.lightGrayElevated


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreItem(
    modifier: Modifier = Modifier,
    item: Genre,
    onGenreClick: () -> Unit,
) {
    val context = LocalContext.current

    Card(
        onClick = onGenreClick,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = context.buildImageUrl(
                    imagePath = item.posterPath,
                    imageSize = "w533_and_h300_bestv2"
                ),
                placeholder = IconResource.fromDrawableResource(R.drawable.movie_placeholder)
                    .asPainterResource(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
            )

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
                                endY = size.height.times(0.8F)
                            )
                        )
                    },
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = item.name,
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

@Composable
fun GenreItemPlaceholder() {
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