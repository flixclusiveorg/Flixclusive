package com.flixclusive.presentation.tv.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardLayoutDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.StandardCardLayout
import coil.compose.AsyncImage
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.presentation.tv.utils.ModifierTvUtils
import com.flixclusive.presentation.utils.ImageRequestCreator.buildImageUrl

val FilmCardWidth = 100.dp
val FilmCardHeight = 150.dp
val FilmCardShape = ShapeDefaults.ExtraSmall
val FilmPadding = ModifierTvUtils.Padding(3.dp)

@Composable
fun FilmCardTv(
    modifier: Modifier = Modifier,
    film: Film,
    onClick: (Film) -> Unit,
    contentPadding: PaddingValues = FilmPadding.getPaddingValues(),
    filmCardHeight: Dp = FilmCardHeight,
    filmCardWidth: Dp = FilmCardWidth,
) {
    val focusedBorderWidth = 3.dp
    val context = LocalContext.current

    val posterImage = context.buildImageUrl(
        imagePath = film.posterImage,
        imageSize = "w220_and_h330_face"
    )

    StandardCardLayout(
        modifier = modifier
            .padding(contentPadding),
        imageCard = {
            CardLayoutDefaults.ImageCard(
                onClick = { onClick(film) },
                interactionSource = it,
                shape = CardDefaults.shape(FilmCardShape),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(
                            width = focusedBorderWidth,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = FilmCardShape
                    )
                ),
                scale = CardDefaults.scale(focusedScale = 1F),
            ) {
                AsyncImage(
                    model = posterImage,
                    error = painterResource(id = R.drawable.movie_placeholder),
                    contentDescription = stringResource(R.string.film_item_content_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(filmCardWidth, filmCardHeight)
                        .graphicsLayer {
                            shape = FilmCardShape
                            clip = true
                        }
                )
            }
        },
        title = {}
    )
}