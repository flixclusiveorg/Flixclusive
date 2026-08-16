package com.flixclusive.feature.mobile.settings.screen.system.logcat

internal data class ParsedFilter(
    val filter: LogFilter,
    val error: String? = null,
)

/**
 * Parses the Android-Studio-style filter query. It never throws: the user is typing, so a half
 * written query has to degrade into something that still shows rows rather than blanking the list.
 * Anything it cannot make sense of becomes a plain substring term and is reported through
 * [ParsedFilter.error].
 */
internal object LogFilterParser {
    private const val REGEX_OPERATOR = '~'
    private const val NEGATION = '-'
    private const val QUOTE = '"'
    private const val ESCAPE = '\\'

    fun parse(query: String): ParsedFilter {
        val (rawTerms, unterminatedQuote) = splitTerms(query)
        if (rawTerms.isEmpty()) {
            return ParsedFilter(LogFilter.MatchAll, error = unterminatedQuote.errorOrNull())
        }

        var error: String? = unterminatedQuote.errorOrNull()
        val terms = rawTerms.map { raw ->
            val parsed = parseTerm(raw)
            if (error == null) error = parsed.error
            parsed.filter
        }

        val filter = if (terms.size == 1) terms.first() else LogFilter.And(terms)
        return ParsedFilter(filter, error = error)
    }

    private fun Boolean.errorOrNull() = if (this) "Unterminated quote" else null

    private fun splitTerms(query: String): Pair<List<String>, Boolean> {
        val terms = mutableListOf<String>()
        val current = StringBuilder()
        var inQuote = false
        var escaped = false

        query.forEach { char ->
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }

                char == ESCAPE && inQuote -> {
                    current.append(char)
                    escaped = true
                }

                char == QUOTE -> {
                    current.append(char)
                    inQuote = !inQuote
                }

                char.isWhitespace() && !inQuote -> {
                    if (current.isNotEmpty()) {
                        terms += current.toString()
                        current.clear()
                    }
                }

                else -> current.append(char)
            }
        }

        if (current.isNotEmpty()) terms += current.toString()
        return terms to inQuote
    }

    private fun parseTerm(raw: String): ParsedFilter {
        val negated = raw.length > 1 && raw[0] == NEGATION
        val body = if (negated) raw.substring(1) else raw

        val parsed = parseUnsignedTerm(body)
        val filter = if (negated) LogFilter.Not(parsed.filter) else parsed.filter
        return ParsedFilter(filter, error = parsed.error)
    }

    private fun parseUnsignedTerm(body: String): ParsedFilter {
        val quoteIndex = body.indexOf(QUOTE)
        val colonIndex = body.indexOf(':')
        val hasFieldSeparator = colonIndex > 0 && (quoteIndex == -1 || colonIndex < quoteIndex)

        if (hasFieldSeparator) {
            val isRegex = body[colonIndex - 1] == REGEX_OPERATOR
            val keyToken = body.substring(0, if (isRegex) colonIndex - 1 else colonIndex)
            val key = FilterKey.fromToken(keyToken)

            if (key != null) {
                val (matcher, error) = toMatcher(body.substring(colonIndex + 1), isRegex = isRegex)
                return ParsedFilter(LogFilter.Field(key, matcher), error = error)
            }
        }

        val (matcher, error) = toMatcher(body, isRegex = false)
        return ParsedFilter(LogFilter.Bare(matcher), error = error)
    }

    private fun toMatcher(
        raw: String,
        isRegex: Boolean,
    ): Pair<ValueMatcher, String?> {
        val isQuoted = raw.length >= 2 && raw.first() == QUOTE && raw.last() == QUOTE
        val literal = when {
            isQuoted -> unescape(raw.substring(1, raw.length - 1))
            raw.startsWith(QUOTE) -> unescape(raw.substring(1))
            else -> raw
        }

        if (isRegex) {
            val regex = runCatching { Regex(literal) }.getOrNull()
                ?: return ValueMatcher.Substring(literal) to "Invalid pattern: $literal"

            return ValueMatcher.Pattern(regex) to null
        }

        return if (isQuoted) ValueMatcher.Exact(literal) to null else ValueMatcher.Substring(literal) to null
    }

    private fun unescape(value: String): String {
        if (!value.contains(ESCAPE)) return value

        val builder = StringBuilder(value.length)
        var escaped = false
        value.forEach { char ->
            when {
                escaped -> {
                    builder.append(char)
                    escaped = false
                }

                char == ESCAPE -> escaped = true
                else -> builder.append(char)
            }
        }

        return builder.toString()
    }
}
