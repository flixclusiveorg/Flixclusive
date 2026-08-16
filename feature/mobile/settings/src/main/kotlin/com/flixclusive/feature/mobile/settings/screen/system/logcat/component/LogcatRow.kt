package com.flixclusive.feature.mobile.settings.screen.system.logcat.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flixclusive.core.presentation.mobile.theme.FlixclusiveTheme
import com.flixclusive.feature.mobile.settings.screen.system.logcat.LogEntry
import com.flixclusive.feature.mobile.settings.screen.system.logcat.LogLevel
import com.flixclusive.feature.mobile.settings.screen.system.logcat.LogcatParser

/**
 * Every row shares one [horizontalScroll] state and one explicit [width]. Both are required:
 * the shared state is what makes the rows pan together, and the identical width is what stops the
 * N scroll nodes writing conflicting extents for that one state on every frame.
 */
@Composable
internal fun LogcatRow(
    entry: LogEntry,
    searchQuery: String,
    isCurrentMatch: Boolean,
    style: TextStyle,
    width: Dp,
    height: Dp,
    horizontalScroll: ScrollState,
    matchStyle: SpanStyle,
    modifier: Modifier = Modifier,
) {
    val display = remember(entry.id, searchQuery, matchStyle) {
        entry.text.highlightOccurrences(query = searchQuery, style = matchStyle)
    }

    val currentMatchColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .height(height)
            .horizontalScroll(horizontalScroll),
    ) {
        Text(
            text = display,
            style = style,
            color = entry.level.color(),
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .width(width)
                .then(if (isCurrentMatch) Modifier.background(currentMatchColor) else Modifier),
        )
    }
}

@Composable
private fun LogLevel.color(): Color =
    when (this) {
        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
        LogLevel.ERROR, LogLevel.ASSERT -> MaterialTheme.colorScheme.error
        LogLevel.INFO -> LocalContentColor.current.copy(alpha = 0.9f)
        LogLevel.DEBUG -> LocalContentColor.current.copy(alpha = 0.65f)
        LogLevel.VERBOSE, LogLevel.SILENT -> LocalContentColor.current.copy(alpha = 0.45f)
    }

internal fun String.highlightOccurrences(
    query: String,
    style: SpanStyle,
): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(this)

    val source = this
    var index = source.indexOf(query, ignoreCase = true)
    if (index < 0) return AnnotatedString(source)

    return buildAnnotatedString {
        append(source)
        while (index >= 0) {
            addStyle(style, index, index + query.length)
            index = source.indexOf(query, startIndex = index + query.length, ignoreCase = true)
        }
    }
}

@Preview
@Composable
private fun LogcatRowPreview() {
    val entries = LogLevel.entries.mapIndexed { index, level ->
        LogcatParser.parse(
            raw = "06-14 09:12:33.123  1234  1256 ${level.letter} Flixclusive: sample ${level.name} line",
            id = index.toLong(),
            previous = null,
        )!!
    }

    val scroll = rememberScrollState()
    val style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)

    FlixclusiveTheme {
        Surface {
            Column {
                entries.forEach { entry ->
                    LogcatRow(
                        entry = entry,
                        searchQuery = "sample",
                        isCurrentMatch = entry.level == LogLevel.ERROR,
                        style = style,
                        width = 600.dp,
                        height = 18.dp,
                        horizontalScroll = scroll,
                        matchStyle = SpanStyle(background = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    )
                }
            }
        }
    }
}
