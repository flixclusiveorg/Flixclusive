package com.flixclusive.core.common.locale

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Returns "MMMM dd, yyyy" formatted string of this date.
 * If the date is null, returns "Unknown".
 * */
fun Date?.toFormattedString(): String? {
    if (this == null) return null

    val formatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return formatter.format(this)
}
