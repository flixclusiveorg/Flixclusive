package com.flixclusive.presentation.home.content

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flixclusive.R
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.presentation.common.Formatter.formatRating
import com.flixclusive.presentation.common.IconResource
import com.flixclusive.presentation.common.ImageRequestCreator.buildImageUrl
import com.flixclusive.presentation.common.UiText
import com.flixclusive.presentation.common.composables.fadingEdge
import com.flixclusive.presentation.common.composables.placeholderEffect
import com.flixclusive.presentation.main.LABEL_START_PADDING
import com.flixclusive.ui.theme.colorOnMediumEmphasis
import com.flixclusive.ui.theme.lightGray
import com.flixclusive.ui.theme.lightGrayElevated
import com.flixclusive.ui.theme.starColor
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeHeader(
    modifier: Modifier = Modifier,
    film: Film,
    onGenreClick: (Genre) -> Unit,
    onFilmClick: (Film) -> Unit,
    onFilmLongClick: (Film) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val backgroundColor = MaterialTheme.colorScheme.background

    var shouldShowTextInsteadOfLogo by remember { mutableStateOf(false) }
    var logoImage: ImageRequest? by remember { mutableStateOf(null) }
    var posterImage: ImageRequest? by remember { mutableStateOf(null) }

    var filmInfo by remember { mutableStateOf("") }

    Box(
        contentAlignment = Alignment.Center
    ) {
        if(filmInfo.isEmpty())
            filmInfo = "${formatRating(film.rating)} | ${film.dateReleased}"

        if(posterImage == null)
            posterImage = context.buildImageUrl(
                imagePath = film.backdropImage,
                imageSize = "w600_and_h900_multi_faces"
            )

        if(logoImage == null)
            logoImage = context.buildImageUrl(
                imagePath = film.logoImage,
                imageSize = "w500"
            )

        AsyncImage(
            model = posterImage,
            placeholder = IconResource.fromDrawableResource(R.drawable.movie_placeholder)
                .asPainterResource(),
            contentDescription = UiText.StringResource(R.string.popular).asString(),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .combinedClickable(
                    onClick = { onFilmClick(film) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFilmLongClick(film)
                    }
                )
        )

        Column(
            modifier = Modifier
                .matchParentSize()
                .statusBarsPadding()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor
                            ),
                            startY = 0F,
                            endY = 1160F
                        )
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box {
                    if(shouldShowTextInsteadOfLogo) {
                        Text(
                            text = film.title,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            softWrap = true,
                            modifier = Modifier.padding(
                                top = 23.dp,
                                start = 5.dp,
                                end = 5.dp
                            )
                        )
                    } else {
                        AsyncImage(
                            model = logoImage,
                            contentDescription = film.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .height(96.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 25.dp),
                            onError = {
                                shouldShowTextInsteadOfLogo = true
                            }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 15.dp)
                ) {
                    Icon(
                        painter = IconResource.fromImageVector(Icons.Rounded.Star).asPainterResource(),
                        contentDescription = UiText.StringResource(R.string.rating_icon).asString(),
                        modifier = Modifier.scale(0.6F),
                        tint = starColor
                    )

                    Text(
                        text = filmInfo,
                        style = MaterialTheme.typography.labelMedium,
                        color = colorOnMediumEmphasis()
                    )
                }

                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    maxItemsInEachRow = 4
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


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeHeaderPlaceholder(
    modifier: Modifier = Modifier
) {
    val listBottomFade = Brush.verticalGradient(0.8f to Color.Red, 0.9f to Color.Transparent)

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .fadingEdge(listBottomFade)
                .drawBehind {
                    drawRect(lightGray)
                }
        ) {}

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LABEL_START_PADDING,
                    vertical = LABEL_START_PADDING
                )
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier
                    .height(90.dp)
                    .fillMaxWidth(0.85F)
                    .placeholderEffect(
                        shape = RoundedCornerShape(15),
                        color = lightGrayElevated
                    )
            )

            Spacer(
                modifier = Modifier
                    .height(25.dp)
                    .fillMaxWidth(0.4F)
                    .padding(top = 15.dp)
                    .placeholderEffect(
                        shape = RoundedCornerShape(100),
                        color = lightGrayElevated
                    )
            )

            FlowRow(
                modifier = Modifier
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                maxItemsInEachRow = 3
            ) {
                repeat(3) {
                    val randomWidth by remember { mutableStateOf(Random.nextInt(60, 120)) }

                    Spacer(
                        modifier = Modifier
                            .height(23.dp)
                            .width(randomWidth.dp)
                            .placeholderEffect(
                                shape = RoundedCornerShape(100),
                                color = lightGrayElevated
                            )
                    )
                }
            }
        }
    }
}