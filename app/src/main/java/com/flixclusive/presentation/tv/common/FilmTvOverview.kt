package com.flixclusive.presentation.tv.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.TvShow
import com.flixclusive.presentation.tv.utils.ComposeTvUtils
import com.flixclusive.presentation.tv.utils.ComposeTvUtils.BoxedEmphasisRating
import com.flixclusive.presentation.utils.ImageRequestCreator.buildImageUrl

@Composable
fun FilmTvOverview(
    modifier: Modifier = Modifier,
    film: Film,
    shouldEllipsize: Boolean = true,
) {
    val context = LocalContext.current

    val colorOnMediumEmphasis = ComposeTvUtils.colorOnMediumEmphasisTv()
    var shouldShowTextInsteadOfLogo by remember { mutableStateOf(false) }

    val filmInfo = remember(film) {
        when(film) {
            is Movie -> listOf(film.runtime.trim())
            is TvShow -> {
                if(film.runtime.contains("|")) {
                    film.runtime.split(" | ")
                } else listOf(film.runtime)
            }
            else -> throw IllegalStateException("Invalid film type!")
        } + listOf(film.dateReleased)
    }
    val filmGenres = remember(film) {
        film.genres.map { it.name }
    }

    val logoImage = context.buildImageUrl(
        imagePath = film.logoImage?.replace("svg", "png"),
        imageSize = "w500"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if(shouldShowTextInsteadOfLogo || film.logoImage == null) {
            Text(
                text = film.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 30.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                modifier = Modifier
                    .fillMaxWidth()
            )
        } else {
            AsyncImage(
                model = logoImage,
                contentDescription = film.title,
                modifier = Modifier
                    .height(80.dp)
                    .fillMaxWidth(0.45F),
                alignment = Alignment.CenterStart,
                onError = {
                    shouldShowTextInsteadOfLogo = true
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(0.85F),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BoxedEmphasisRating(film.rating)

            filmInfo.forEach { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colorOnMediumEmphasis
                )
            }
        }

        ComposeTvUtils.DotSeparatedText(
            texts = filmGenres,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.fillMaxWidth(0.85F)
        )

        if(film.overview != null) {
            Text(
                text = film.overview!!,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = colorOnMediumEmphasis,
                maxLines = if(shouldEllipsize) 3 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(if(shouldEllipsize) 0.55F else 0.75F)
            )
        }
    }
}