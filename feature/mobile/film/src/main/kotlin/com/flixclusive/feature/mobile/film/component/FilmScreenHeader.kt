package com.flixclusive.feature.mobile.film.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.imageLoader
import com.flixclusive.core.theme.starColor
import com.flixclusive.core.ui.common.util.buildImageUrl
import com.flixclusive.core.ui.common.util.formatTvRuntime
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.mobile.component.film.GenreButton
import com.flixclusive.core.ui.common.util.formatMinutes
import com.flixclusive.core.ui.common.util.formatRating
import com.flixclusive.feature.mobile.film.R
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.Genre
import com.flixclusive.model.film.TvShow
import com.flixclusive.core.locale.R as LocaleR

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilmScreenHeader(
    onNavigateClick: () -> Unit,
    onGenreClick: (Genre) -> Unit,
    film: Film
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val context = LocalContext.current

    val imageHeight = 480

    Box {
        AsyncImage(
            model = context.buildImageUrl(
                imagePath = film.posterImage,
                imageSize = "original"
            ),
            imageLoader = LocalContext.current.imageLoader,
            placeholder = painterResource(R.drawable.movie_placeholder),
            error = painterResource(R.drawable.movie_placeholder),
            contentDescription = stringResource(
                id = LocaleR.string.poster_content_description,
                film.title
            ),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .height(imageHeight.dp)
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(
                            0F to Color.Transparent,
                            0.9F to backgroundColor,
                        )
                    )
                }
        )

        IconButton(
            onClick = onNavigateClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 15.dp)
                .padding(top = 5.dp)
                .clip(RoundedCornerShape(25))
                .background(color = MaterialTheme.colorScheme.surface.onMediumEmphasis())
        ) {
            Icon(
                painter = painterResource(R.drawable.left_arrow),
                contentDescription = stringResource(LocaleR.string.navigate_up),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = imageHeight.times(0.95).dp)
            ) {
                Text(
                    text = film.title,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Start,
                    softWrap = true,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(
                            start = 5.dp,
                            end = 5.dp
                        )
                )
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = stringResource(LocaleR.string.rating_icon),
                    modifier = Modifier.scale(0.6F),
                    tint = starColor
                )

                Text(
                    text = buildAnnotatedString {
                        val separator = " | "
                        withStyle(
                            style = SpanStyle(fontSize = 12.sp)
                        ) {
                            append(formatRating(film.rating).asString(context))

                            if(film is TvShow) {
                                append(separator)
                                append(
                                    formatTvRuntime(
                                        context = context,
                                        show = film,
                                        separator = separator
                                    )
                                )
                            } else {
                                append(separator)
                                append(formatMinutes(film.runtime).asString(context))
                                append(separator)
                            }

                            append(film.parsedReleaseDate ?: "No release date")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current.onMediumEmphasis(),
                    textAlign = TextAlign.Start,
                    softWrap = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }

            FlowRow(
                verticalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.Top
                ),
                modifier = Modifier
                    .padding(
                        top = 8.dp,
                        bottom = 2.dp
                    )
            ) {
                film.genres.forEach {
                    GenreButton(
                        genre = it,
                        onClick = onGenreClick
                    )
                }
            }
        }
    }
}