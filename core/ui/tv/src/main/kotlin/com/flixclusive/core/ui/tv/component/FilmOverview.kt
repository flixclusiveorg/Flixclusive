package com.flixclusive.core.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.flixclusive.core.ui.common.util.formatTvRuntime
import com.flixclusive.core.ui.common.util.onMediumEmphasis
import com.flixclusive.core.ui.tv.FilmLogo
import com.flixclusive.core.util.film.formatMinutes
import com.flixclusive.model.tmdb.Film
import com.flixclusive.model.tmdb.TvShow

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FilmOverview(
    modifier: Modifier = Modifier,
    film: Film,
    shouldEllipsize: Boolean = true,
) {
    val context = LocalContext.current

    val colorOnMediumEmphasis = LocalContentColor.current.onMediumEmphasis()

    val filmInfo = remember(film) {
        val infoList = mutableListOf<String>()
        infoList.apply {
            if(film is TvShow) {
                addAll(
                    formatTvRuntime(
                        context = context,
                        show = film,
                        separator = ","
                    ).split(",")
                )
            } else {
                add(formatMinutes(film.runtime).asString(context))
            }

            add(film.dateReleased)
        }.toList()
            .filterNot { it.isEmpty() }
    }
    val filmGenres = remember(film) {
        film.genres.map { it.name }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilmLogo(
            film = film,
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth(0.45F)
        )

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

        DotSeparatedText(
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