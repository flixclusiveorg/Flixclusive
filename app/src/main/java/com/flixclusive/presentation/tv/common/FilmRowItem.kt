package com.flixclusive.presentation.tv.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
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
import com.flixclusive.presentation.utils.ImageRequestCreator.buildImageUrl

val FilmCardShape = ShapeDefaults.ExtraSmall
val FilmPadding = 3.dp
val FilmDefaultHeight = 150.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FilmRowItem(
    modifier: Modifier = Modifier,
    film: Film,
    onClick: (Film) -> Unit,
    contentPadding: PaddingValues = PaddingValues(FilmPadding),
    filmCardHeight: Dp = FilmDefaultHeight,
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
                    placeholder = painterResource(R.drawable.movie_placeholder),
                    error = painterResource(id = R.drawable.movie_placeholder),
                    contentDescription = stringResource(R.string.film_item_content_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(filmCardHeight)
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