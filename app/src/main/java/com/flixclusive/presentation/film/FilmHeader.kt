package com.flixclusive.presentation.film

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.presentation.common.Formatter
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.main.LABEL_START_PADDING
import com.flixclusive.ui.theme.colorOnMediumEmphasis
import com.flixclusive.ui.theme.starColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilmHeader(
    onNavigateClick: () -> Unit,
    onGenreClick: (Genre) -> Unit,
    film: Film
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val context = LocalContext.current
    val posterImage by remember { mutableStateOf(
        context.buildImageUrl(
            imagePath = film.posterImage,
            imageSize = "original"
        )
    ) }

    Box(
        modifier = Modifier.heightIn(min = 480.dp)
    ) {
        AsyncImage(
            model = posterImage,
            placeholder = IconResource.fromDrawableResource(R.drawable.movie_placeholder).asPainterResource(),
            contentDescription = String.format(UiText.StringResource(R.string.poster_content_description).asString(), film.title),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .height(480.dp)
                .fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 480.dp,
                    max = 680.dp
                )
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor
                            ),
                            startY = 0F,
                            endY = size.height.times(0.9F)
                        )
                    )
                }
                .padding(horizontal = LABEL_START_PADDING)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(
                        top = 5.dp,
                        bottom = 200.dp
                    )
            ) {
                IconButton(
                    onClick = onNavigateClick,
                    modifier = Modifier
                        .clip(RoundedCornerShape(25))
                        .background(color = colorOnMediumEmphasis(MaterialTheme.colorScheme.surface))
                ) {
                    Icon(
                        painter = IconResource.fromDrawableResource(R.drawable.left_arrow)
                            .asPainterResource(),
                        contentDescription = UiText.StringResource(R.string.navigate_up).asString(),
                    )
                }
            }

            Column {
                Text(
                    text = UiText.StringValue(film.title).asString(),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Start,
                    softWrap = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 5.dp,
                            end = 5.dp
                        )
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        painter = IconResource.fromImageVector(Icons.Rounded.Star).asPainterResource(),
                        contentDescription = UiText.StringResource(R.string.rating).asString(),
                        modifier = Modifier.scale(0.6F),
                        tint = starColor
                    )

                    Text(
                        text = "${Formatter.formatRating(film.rating)} | ${film.runtime} | ${film.dateReleased}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorOnMediumEmphasis(),
                        textAlign = TextAlign.Start,
                        softWrap = true,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                FlowRow(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    film.genres.forEach {
                        OutlinedButton(
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorOnMediumEmphasis()),
                            onClick = {
                                onGenreClick(
                                    it.copy(mediaType = film.filmType.type)
                                )
                            },
                            contentPadding = PaddingValues(
                                horizontal = 15.dp,
                                vertical = 5.dp
                            ),
                            elevation = ButtonDefaults.buttonElevation(8.dp),
                            modifier = Modifier
                                .padding(horizontal = 5.dp)
                                .defaultMinSize(
                                    minHeight = 1.dp,
                                    minWidth = 1.dp
                                )
                        ) {
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}