package com.flixclusive.feature.mobile.settings.screen.system.logcat

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import strikt.assertions.startsWith

class LogcatParserTest {
    private fun parse(
        raw: String,
        id: Long = 0L,
        previous: LogEntry? = null,
    ) = LogcatParser.parse(raw = raw, id = id, previous = previous)

    @Test
    fun `should parse every field of a canonical threadtime line`() {
        val entry = parse("06-14 09:12:33.123  1234  1256 D MyTag  : hello world")

        expectThat(entry).isNotNull().and {
            get { date }.isEqualTo("06-14")
            get { time }.isEqualTo("09:12:33.123")
            get { pid }.isEqualTo(1234)
            get { tid }.isEqualTo(1256)
            get { level }.isEqualTo(LogLevel.DEBUG)
            get { tag }.isEqualTo("MyTag")
            get { message }.isEqualTo("hello world")
            get { isContinuation }.isFalse()
        }
    }

    @Test
    fun `should parse a year-prefixed line`() {
        val entry = parse("2026-06-14 09:12:33.123  1234  1256 I Tag: body")

        expectThat(entry).isNotNull().and {
            get { date }.isEqualTo("06-14")
            get { level }.isEqualTo(LogLevel.INFO)
            get { message }.isEqualTo("body")
        }
    }

    @Test
    fun `should parse a tag containing spaces`() {
        val entry = parse("06-14 09:12:33.123  1234  1256 W My Tag: body")

        expectThat(entry).isNotNull().get { tag }.isEqualTo("My Tag")
    }

    @Test
    fun `should keep colons inside the message`() {
        val entry = parse("06-14 09:12:33.123  1234  1256 E Tag: host:8080 failed: reset")

        expectThat(entry).isNotNull().get { message }.isEqualTo("host:8080 failed: reset")
    }

    @Test
    fun `should map every level letter`() {
        LogLevel.entries.forEach { level ->
            val entry = parse("06-14 09:12:33.123  1  2 ${level.letter} Tag: body")

            expectThat(entry).isNotNull().get { this.level }.isEqualTo(level)
        }
    }

    @Test
    fun `should reject an unknown level letter`() {
        expectThat(parse("06-14 09:12:33.123  1  2 X Tag: body")).isNull()
    }

    @Test
    fun `should drop buffer dividers`() {
        expectThat(parse("--------- beginning of crash")).isNull()
    }

    @Test
    fun `should drop blank lines`() {
        expectThat(parse("   ")).isNull()
    }

    @Test
    fun `should emit a continuation inheriting the previous header`() {
        val previous = parse("06-14 09:12:33.123  1234  1256 E Crash: java.lang.IllegalStateException")
        val continuation = parse(
            raw = "\tat com.flixclusive.Foo.bar(Foo.kt:42)",
            id = 1L,
            previous = previous,
        )

        expectThat(continuation).isNotNull().and {
            get { id }.isEqualTo(1L)
            get { pid }.isEqualTo(1234)
            get { tid }.isEqualTo(1256)
            get { level }.isEqualTo(LogLevel.ERROR)
            get { tag }.isEqualTo("Crash")
            get { message }.isEqualTo("at com.flixclusive.Foo.bar(Foo.kt:42)")
            get { isContinuation }.isTrue()
            get { messageStart }.isEqualTo(previous!!.messageStart)
        }
    }

    @Test
    fun `should drop a headerless line when there is no previous entry`() {
        expectThat(parse("\tat com.flixclusive.Foo.bar(Foo.kt:42)")).isNull()
    }

    @Test
    fun `should truncate a message beyond the line limit`() {
        val long = "x".repeat(LogcatParser.MAX_LINE_CHARS + 500)
        val entry = parse("06-14 09:12:33.123  1  2 D Tag: $long")

        expectThat(entry)
            .isNotNull()
            .get { message }
            .get { length }
            .isEqualTo(LogcatParser.MAX_LINE_CHARS + 1)
    }

    @Test
    fun `should point messageStart at the first message character`() {
        val entry = parse("06-14 09:12:33.123  1234  1256 D MyTag: hello")!!

        expectThat(entry.text.substring(entry.messageStart)).isEqualTo("hello")
        expectThat(entry.text).startsWith("09:12:33.123  1234-1256  D  MyTag")
    }

    @Test
    fun `should align a continuation under the message column`() {
        val previous = parse("06-14 09:12:33.123  1234  1256 E Crash: header")
        val continuation = parse(raw = "frame", id = 1L, previous = previous)!!

        expectThat(continuation.text.substring(continuation.messageStart)).isEqualTo("frame")
        expectThat(continuation.text.take(continuation.messageStart).isBlank()).isTrue()
    }
}
