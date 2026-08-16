package com.flixclusive.feature.mobile.settings.screen.system.logcat

import androidx.compose.runtime.Immutable

internal enum class LogLevel(
    val letter: Char,
) {
    VERBOSE('V'),
    DEBUG('D'),
    INFO('I'),
    WARN('W'),
    ERROR('E'),
    ASSERT('F'),
    SILENT('S'),
    ;

    companion object {
        fun fromLetter(letter: Char): LogLevel? = entries.firstOrNull { it.letter == letter }

        /**
         * Resolves a user-typed level token. Accepts the single letter, the full name, or any
         * unambiguous prefix of it, so `level:e`, `level:err` and `level:ERROR` all mean the same
         * thing the way they do in Android Studio.
         */
        fun fromToken(token: String): LogLevel? {
            if (token.isEmpty()) return null

            val normalized = token.uppercase()
            if (normalized.length == 1) {
                return fromLetter(normalized[0])
            }

            val matches = entries.filter { it.name.startsWith(normalized) }
            return matches.singleOrNull()
        }
    }
}

@Immutable
internal data class LogEntry(
    val id: Long,
    val date: String,
    val time: String,
    val pid: Int,
    val tid: Int,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val isContinuation: Boolean,
    val text: String,
    val messageStart: Int,
)
