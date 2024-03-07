package com.flixclusive.provider.util

import java.text.SimpleDateFormat
import java.util.Locale

fun String.replaceWhitespaces(toReplace: String) = replace(
    Regex("[\\s_]+"),
    toReplace
)

fun String?.toValidReleaseDate(format: String = "MMMM d, yyyy"): String? {
    if(isNullOrBlank())
        return null

    val inputFormat = SimpleDateFormat(format, Locale.US)
    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    return try {
        val date = inputFormat.parse(this)
        outputFormat.format(date)
    } catch (e: Exception) {
        throw Exception("Cannot parse release date of show.")
    }
}