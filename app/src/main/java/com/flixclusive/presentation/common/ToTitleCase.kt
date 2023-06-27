package com.flixclusive.presentation.common

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