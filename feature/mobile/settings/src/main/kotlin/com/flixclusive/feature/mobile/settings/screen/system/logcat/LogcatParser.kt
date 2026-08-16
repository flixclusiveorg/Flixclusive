package com.flixclusive.feature.mobile.settings.screen.system.logcat

internal object LogcatParser {
    const val TAG_WIDTH = 23
    const val MAX_LINE_CHARS = 2_000

    private const val DIVIDER_PREFIX = "---------"
    private const val ELLIPSIS = "…"

    private val THREADTIME =
        Regex(
            """^(?:\d{4}-)?(\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEFS])\s+(.*?)\s*:\s?(.*)$""",
        )

    /**
     * [previous] is passed in rather than held as parser state so this stays a pure function. A line
     * that carries no header is a continuation of the entry before it — a stack trace frame, say —
     * and is emitted as its own entry rather than being folded into [previous]'s message, because a
     * row renders exactly one line.
     */
    fun parse(
        raw: String,
        id: Long,
        previous: LogEntry?,
    ): LogEntry? {
        if (raw.isBlank()) return null
        if (raw.startsWith(DIVIDER_PREFIX)) return null

        val match = THREADTIME.matchEntire(raw)
        if (match == null) {
            return previous?.let { continuationOf(it, id = id, message = raw.trim()) }
        }

        val (date, time, pid, tid, levelLetter, tag, message) = match.destructured
        val level = LogLevel.fromLetter(levelLetter[0]) ?: return null
        val truncated = message.truncate()

        val prefix = buildPrefix(time = time, pid = pid, tid = tid, level = level, tag = tag)

        return LogEntry(
            id = id,
            date = date,
            time = time,
            pid = pid.toIntOrNull() ?: return null,
            tid = tid.toIntOrNull() ?: return null,
            level = level,
            tag = tag,
            message = truncated,
            isContinuation = false,
            text = prefix + truncated,
            messageStart = prefix.length,
        )
    }

    private fun continuationOf(
        previous: LogEntry,
        id: Long,
        message: String,
    ): LogEntry {
        val truncated = message.truncate()

        return previous.copy(
            id = id,
            message = truncated,
            isContinuation = true,
            text = " ".repeat(previous.messageStart) + truncated,
        )
    }

    private fun buildPrefix(
        time: String,
        pid: String,
        tid: String,
        level: LogLevel,
        tag: String,
    ): String =
        buildString {
            append(time)
            append("  ")
            append(pid)
            append('-')
            append(tid)
            append("  ")
            append(level.letter)
            append("  ")
            append(tag.take(TAG_WIDTH).padEnd(TAG_WIDTH))
            append("  ")
        }

    private fun String.truncate(): String =
        if (length <= MAX_LINE_CHARS) this else take(MAX_LINE_CHARS) + ELLIPSIS
}
