package com.flixclusive.presentation.utils

import android.os.Build
import com.flixclusive.R
import com.flixclusive.common.Constants.FILM_TV_SHOW_TITLE_FORMAT
import com.flixclusive.common.UiText
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.tmdb.Film
import com.flixclusive.domain.model.tmdb.FilmType
import com.flixclusive.domain.model.tmdb.Genre
import com.flixclusive.domain.model.tmdb.TMDBEpisode
import com.flixclusive.domain.utils.WatchHistoryUtils.getNextEpisodeToWatch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import java.util.concurrent.TimeUnit

object FormatterUtils {
    fun formatMinutes(totalMinutes: Int?): String {
        if (totalMinutes == null)
            return "No runtime"

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val hoursText = if (hours > 0) "${hours}h " else ""
        val minutesText = if (minutes > 0) "${minutes}m" else ""

        return (hoursText + minutesText).trim()
    }

    fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) {
            return "No release date"
        }

        val locale = Locale.US

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val date = LocalDate.parse(dateString)
            val formatter = DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMMM d, yyyy")
                .toFormatter(locale)

            return date.format(formatter)
        }

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", locale)
        val outputFormat = SimpleDateFormat("MMMM d, yyyy", locale)

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

    fun formatGenreIds(
        genreIds: List<Int>,
        genresList: List<Genre>
    ): List<Genre> {
        val genreMap = genresList.associateBy({ it.id }, { it })
        return genreIds.mapNotNull { genreMap[it] }
    }

    fun String.getTypeFromQuery(): String {
        return if(contains("tv?")) "tv" else "movie"
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
                val lastEpisodeWatched =
                    getNextEpisodeToWatch(
                        watchHistoryItem
                    )
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
            FilmType.MOVIE -> film.title
            FilmType.TV_SHOW -> String.format(
                FILM_TV_SHOW_TITLE_FORMAT,
                episode!!.season,
                episode.episode,
                episode.title
            )
        }
    }

    fun String.toTitleCase(): String {
        var space = true
        val builder = StringBuilder(this)
        val len = builder.length
        for (i in 0 until len) {
            val c = builder[i]
            if (space) {
                if (!Character.isWhitespace(c)) {
                    // Convert to title case and switch out of whitespace mode.
                    builder.setCharAt(i, c.titlecaseChar())
                    space = false
                }
            } else if (Character.isWhitespace(c)) {
                space = true
            } else {
                builder.setCharAt(i, c.lowercaseChar())
            }
        }
        return builder.toString()
    }
}