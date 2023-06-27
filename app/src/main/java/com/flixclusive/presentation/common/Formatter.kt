package com.flixclusive.presentation.common

import android.os.Build
import com.flixclusive.R
import com.flixclusive.common.Constants.FILM_MOVIE_TITLE_FORMAT
import com.flixclusive.common.Constants.FILM_TV_SHOW_TITLE_FORMAT
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.GENRES_LIST
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.domain.model.tmdb.Movie
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatter {
    fun formatMinutes(totalMinutes: Int?): String {
        if (totalMinutes == null)
            return "No runtime"

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val hoursText = if (hours > 0) "${hours}h " else ""
        val minutesText = if (minutes > 0) "${minutes}m" else ""

        return hoursText + minutesText
    }

    fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) {
            return "No release date"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val date = LocalDate.parse(dateString)
            val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
            return date.format(formatter)
        }

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)

        val date = inputFormat.parse(dateString)
        return date?.let {
            outputFormat.format(it)
        } ?: "No release date"
    }

    fun formatAirDates(
        firstAirDate: String,
        lastAirDate: String,
        inProduction: Boolean?,
    ): String {
        if (firstAirDate.isEmpty() && lastAirDate.isEmpty()) {
            return "No air dates"
        }

        val firstYear = firstAirDate.substring(0, 4)
        val lastYear = if (inProduction == true) "present" else lastAirDate.substring(0, 4)

        return "$firstYear-$lastYear"
    }

    fun formatRating(number: Double): String {
        return if (number % 1 == 0.0) {
            String.format("%.1f", number)
        } else {
            String.format("%.2f", number)
        }
    }

    fun formatGenreIds(genreIds: List<Int>): List<Genre> {
        val genreMap = GENRES_LIST.associateBy({ it.id }, { it })
        return genreIds.mapNotNull { genreMap[it] }
    }

    fun Long.formatMinSec(isInHours: Boolean = false): String {
        return if (this <= 0L && isInHours) {
            "00:00:00"
        } else if(this <= 0L) {
            "00:00"
        } else {
            val hours = TimeUnit.MILLISECONDS.toHours(this)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(this) -
                    TimeUnit.HOURS.toMinutes(hours)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(this) -
                    TimeUnit.MINUTES.toSeconds(minutes) -
                    TimeUnit.HOURS.toSeconds(hours)

            if (hours > 0 || isInHours) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    }

    fun formatPlayButtonLabel(
        watchHistoryItem: WatchHistoryItem?,
    ): UiText {
        if(watchHistoryItem == null)
            return UiText.StringResource(R.string.watch)

        return when(watchHistoryItem.film.filmType) {
            FilmType.MOVIE -> {
                if(watchHistoryItem.episodesWatched.last().isFinished) {
                    UiText.StringResource(R.string.watch_again)
                } else {
                    UiText.StringResource(R.string.continue_watching)
                }
            }
            FilmType.TV_SHOW -> {
                val lastEpisodeWatched = Functions.getNextEpisodeToWatch(watchHistoryItem)
                val season = lastEpisodeWatched.first
                val episode = lastEpisodeWatched.second

                if(season == null) {
                    UiText.StringResource(R.string.watch_again)
                } else {
                    UiText.StringValue("Continue Watching S${season} E${episode}")
                }
            }
        }
    }

    fun formatPlayerTitle(
        film: Film,
        episode: TMDBEpisode? = null
    ): String {
        return when(film.filmType) {
            FilmType.MOVIE -> String.format(FILM_MOVIE_TITLE_FORMAT, film.title, (film as Movie).releaseDate.split("-")[0])
            FilmType.TV_SHOW -> String.format(FILM_TV_SHOW_TITLE_FORMAT, film.title, episode!!.season, episode.episode, episode.title)
        }
    }
}