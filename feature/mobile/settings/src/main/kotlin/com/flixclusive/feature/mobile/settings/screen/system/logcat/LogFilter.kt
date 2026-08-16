package com.flixclusive.feature.mobile.settings.screen.system.logcat

import androidx.compose.runtime.Immutable

internal enum class FilterKey {
    TAG,
    MESSAGE,
    PACKAGE,
    LEVEL,
    PID,
    TID,
    ;

    companion object {
        fun fromToken(token: String): FilterKey? =
            when (token.lowercase()) {
                "tag" -> TAG
                "message", "msg" -> MESSAGE
                "package", "pkg" -> PACKAGE
                "level" -> LEVEL
                "pid" -> PID
                "tid" -> TID
                else -> null
            }
    }
}

internal sealed interface ValueMatcher {
    fun matches(candidate: String): Boolean

    @Immutable
    data class Substring(
        val value: String,
    ) : ValueMatcher {
        override fun matches(candidate: String) = candidate.contains(value, ignoreCase = true)
    }

    @Immutable
    data class Exact(
        val value: String,
    ) : ValueMatcher {
        override fun matches(candidate: String) = candidate.equals(value, ignoreCase = true)
    }

    @Immutable
    data class Pattern(
        val regex: Regex,
    ) : ValueMatcher {
        override fun matches(candidate: String) = regex.containsMatchIn(candidate)
    }
}

/**
 * The pid and package name are injected rather than read from [android.os.Process] inside the
 * matcher so the whole filter layer stays free of Android types and testable on the JVM.
 */
internal data class LogFilterContext(
    val ownPid: Int,
    val ownPackageName: String,
)

internal sealed interface LogFilter {
    data object MatchAll : LogFilter

    @Immutable
    data class Not(
        val term: LogFilter,
    ) : LogFilter

    @Immutable
    data class And(
        val terms: List<LogFilter>,
    ) : LogFilter

    @Immutable
    data class Field(
        val key: FilterKey,
        val matcher: ValueMatcher,
    ) : LogFilter

    @Immutable
    data class Bare(
        val matcher: ValueMatcher,
    ) : LogFilter
}

internal fun LogFilter.matches(
    entry: LogEntry,
    context: LogFilterContext,
): Boolean =
    when (this) {
        is LogFilter.MatchAll -> true
        is LogFilter.Not -> !term.matches(entry, context)
        is LogFilter.And -> terms.all { it.matches(entry, context) }
        is LogFilter.Bare -> matcher.matches(entry.text)
        is LogFilter.Field -> matchesField(entry, context)
    }

private fun LogFilter.Field.matchesField(
    entry: LogEntry,
    context: LogFilterContext,
): Boolean =
    when (key) {
        FilterKey.TAG -> matcher.matches(entry.tag)
        FilterKey.MESSAGE -> matcher.matches(entry.message)
        FilterKey.PID -> matchesNumber(entry.pid)
        FilterKey.TID -> matchesNumber(entry.tid)
        FilterKey.LEVEL -> matchesLevel(entry)
        FilterKey.PACKAGE -> matchesPackage(entry, context)
    }

private fun LogFilter.Field.matchesNumber(value: Int): Boolean {
    val expected = matcher.literalOrNull()?.toIntOrNull() ?: return false
    return value == expected
}

/**
 * `level:W` means WARN *and above*, matching Android Studio. A regex matcher falls back to matching
 * the level letter so `level~:[WE]` still does something sensible.
 */
private fun LogFilter.Field.matchesLevel(entry: LogEntry): Boolean {
    val literal = matcher.literalOrNull()
        ?: return matcher.matches(entry.level.letter.toString())

    val minimum = LogLevel.fromToken(literal) ?: return false
    return entry.level.ordinal >= minimum.ordinal
}

/**
 * An unprivileged app can only speak for its own package, so any literal is checked against this
 * app's package name and then narrowed to this app's records. `package:mine` skips the name check.
 */
private fun LogFilter.Field.matchesPackage(
    entry: LogEntry,
    context: LogFilterContext,
): Boolean {
    val isOwnRecord = entry.pid == context.ownPid
    val literal = matcher.literalOrNull() ?: return isOwnRecord && matcher.matches(context.ownPackageName)

    if (literal.equals(MINE, ignoreCase = true)) return isOwnRecord

    return isOwnRecord && matcher.matches(context.ownPackageName)
}

private fun ValueMatcher.literalOrNull(): String? =
    when (this) {
        is ValueMatcher.Substring -> value
        is ValueMatcher.Exact -> value
        is ValueMatcher.Pattern -> null
    }

internal const val MINE = "mine"
