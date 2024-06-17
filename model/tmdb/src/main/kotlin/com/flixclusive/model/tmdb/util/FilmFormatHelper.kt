package com.flixclusive.model.tmdb.util

import android.os.Build
import com.flixclusive.model.tmdb.Genre
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

internal fun formatDate(dateString: String?): String {
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


fun formatGenreIds(
    genreIds: List<Int>,
    genresList: List<Genre>
): List<Genre> {
    val genreMap = genresList.associateBy({ it.id }, { it })
    return genreIds.mapNotNull { genreMap[it] }
}