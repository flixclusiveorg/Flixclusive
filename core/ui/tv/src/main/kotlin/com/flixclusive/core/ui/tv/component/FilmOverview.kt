package com.flixclusive.core.ui.tv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
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
import com.flixclusive.core.locale.UiText
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.core.ui.common.util.formatMinutes
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.util.getNextEpisodeToWatch
import com.flixclusive.model.film.Film
import com.flixclusive.model.film.TvShow

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FilmOverview(
    modifier: Modifier = Modifier,
    film: Film,
    watchHistoryItem: WatchHistoryItem?,
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

            film.parsedReleaseDate?.let(::add)
        }.toList()
            .filterNot { it.isEmpty() }
    }
    val filmGenres = remember(film) {
        film.genres.map { it.name }
    }

    val lastWatchedEpisode = watchHistoryItem?.episodesWatched?.lastOrNull()
    var progress: Float? by remember(watchHistoryItem) {
        if (lastWatchedEpisode == null)
            return@remember mutableStateOf(null)

        val percentage = if(lastWatchedEpisode.durationTime == 0L) {
            0F
        } else {
            lastWatchedEpisode.watchTime.toFloat() / lastWatchedEpisode.durationTime.toFloat()
        }

        mutableStateOf(percentage)
    }

    val itemLabel = remember(watchHistoryItem) {
        if (watchHistoryItem == null)
            return@remember null

        if(film.filmType == FilmType.TV_SHOW) {
            val nextEpisodeWatched = getNextEpisodeToWatch(watchHistoryItem)
            val season = nextEpisodeWatched.first
            val episode = nextEpisodeWatched.second

            val lastEpisodeIsNotSameWithNextEpisodeToWatch = lastWatchedEpisode?.episodeNumber != episode

            if(lastEpisodeIsNotSameWithNextEpisodeToWatch)
                progress = 0F

            UiText.StringValue("S${season} E${episode}")
        } else {
            val watchTime = watchHistoryItem.episodesWatched.last().watchTime
            val watchTimeInSeconds = (watchTime / 1000).toInt()
            val watchTimeInMinutes = watchTimeInSeconds / 60

            formatMinutes(totalMinutes = watchTimeInMinutes)
        }
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

        if (progress != null) {
            Box(
                modifier = Modifier
                    .height(60.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = itemLabel!!.asString(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp
                        ),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )

                    CustomLinearProgressIndicator(
                        progress = progress!!,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.onMediumEmphasis(0.4F),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                    )
                }
            }
        }
        else if(film.overview != null) {
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