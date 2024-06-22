package com.flixclusive.core.util.film

import android.os.Build
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * Determines whether the given date string represents a date in the future.
 *
 * @param dateString The date string to check. It should be in the format "yyyy-MM-dd" or "MMMM d, yyyy".
 * @return `true` if the date is in the future, `false` otherwise.
 */
fun isDateInFuture(dateString: String): Boolean {
    val locale = Locale.US

    val format = if(dateString.contains(",")) {
        "MMMM d, yyyy"
    } else if(dateString.contains("-")) {
        "yyyy-MM-dd"
    } else ""

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val date = try {
            LocalDate.parse(dateString)
        } catch (e: DateTimeParseException) {
            val formatter = DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(format)
                .toFormatter(locale)

            LocalDate.parse(dateString, formatter)
        }

        date.isAfter(LocalDate.now())
    } else {
        val formatter = SimpleDateFormat(format, locale)
        val currentDate = Calendar.getInstance().time
        val date = formatter.parse(dateString)
        date?.after(currentDate) ?: false
    }
}

fun String.replaceTypeInUrl(type: String): String {
    val pattern = Pattern.compile("(?<=/)[a-z]+(?=\\?)")
    val matcher = pattern.matcher(this)

    if (matcher.find()) {
        return matcher.replaceFirst(type)
    }

    return this
}

fun String.extractYear(): Int? {
    val yearPattern = """\b(\d{4})\b""".toRegex()
    return yearPattern.find(this)?.value?.toIntOrNull()
}