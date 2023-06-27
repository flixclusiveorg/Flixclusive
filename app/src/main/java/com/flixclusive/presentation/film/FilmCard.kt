package com.flixclusive.presentation.film

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.common.UiText
import com.flixclusive.ui.theme.colorOnMediumEmphasis

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilmCard(
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier.fillMaxSize(),
    shouldShowTitle: Boolean = true,
    film: Film,
    onClick: (Film) -> Unit,
    onLongClick: (Film) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val posterImage: ImageRequest = context.buildImageUrl(
        imagePath = film.posterImage,
        imageSize = "w220_and_h330_face"
    )

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 3.dp)
            .combinedClickable(
                onClick = { onClick(film) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick(film)
                }
            )
    ) {
        AsyncImage(
            model = posterImage,
            placeholder = IconResource.fromDrawableResource(R.drawable.movie_placeholder)
                .asPainterResource(),
            contentDescription = UiText.StringResource(R.string.film_item_content_description).asString(),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(200.dp)
                .padding(3.dp)
                .graphicsLayer {
                    shape = RoundedCornerShape(5)
                    clip = true
                }
        )

        if(shouldShowTitle) {
            Text(
                text = film.title,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                color = colorOnMediumEmphasis(),
                maxLines = 1,
                modifier = Modifier
                    .padding(vertical = 5.dp)
            )
        }
    }
}