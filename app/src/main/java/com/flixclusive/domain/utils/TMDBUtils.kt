package com.flixclusive.domain.utils

import android.os.Build
import com.flixclusive.data.dto.tmdb.tv.TvShowSeasonsPreview
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale

object TMDBUtils {
    fun filterOutZeroSeasons(seasons: List<TvShowSeasonsPreview>): List<TvShowSeasonsPreview> {
        return seasons.filter { it.seasonNumber != 0 }
    }

    fun filterOutUnreleasedRecommendations(recommendations: List<TMDBSearchItem>): List<TMDBSearchItem> {
        return recommendations
            .filterNot { isDateInFuture(it.dateReleased) || it.posterImage.isNullOrEmpty() }
    }

    /**
     * Determines whether the given date string represents a date in the future.
     *
     * @param dateString The date string to check. It should be in the format "yyyy-MM-dd" or "MMMM d, yyyy".
     * @return `true` if the date is in the future, `false` otherwise.
     */
    fun isDateInFuture(dateString: String?): Boolean {
        if(dateString == null || dateString == "No release date" || dateString.isEmpty())
            return true

        val format = if(dateString.contains(",")) {
            "MMMM d, yyyy"
        } else if(dateString.contains("-")) {
            "yyyy-MM-dd"
        } else ""

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val date = try {
                LocalDate.parse(dateString)
            } catch (e: DateTimeParseException) {
                val formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH)
                LocalDate.parse(dateString, formatter)
            }

            date.isAfter(LocalDate.now())
        } else {
            val formatter = SimpleDateFormat(format, Locale.US)
            val currentDate = Calendar.getInstance().time
            val date = formatter.parse(dateString)
            date?.after(currentDate) ?: false
        }
    }
}