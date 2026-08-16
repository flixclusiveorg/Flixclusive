package com.flixclusive.feature.mobile.settings.screen.system.logcat

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class LogFilterMatchTest {
    private val context = LogFilterContext(ownPid = 1234, ownPackageName = "com.flixclusive")

    private fun entry(
        pid: Int = 1234,
        tid: Int = 1256,
        level: LogLevel = LogLevel.DEBUG,
        tag: String = "Foo",
        message: String = "body",
    ) = LogcatParser.parse(
        raw = "06-14 09:12:33.123  $pid  $tid ${level.letter} $tag: $message",
        id = 0L,
        previous = null,
    )!!

    private fun matches(
        query: String,
        entry: LogEntry,
    ) = LogFilterParser.parse(query).filter.matches(entry, context)

    @Test
    fun `should match a tag case-insensitively`() {
        expectThat(matches("tag:foo", entry(tag = "Foo"))).isTrue()
    }

    @Test
    fun `should require a full match for a quoted tag`() {
        expectThat(matches("""tag:"foo"""", entry(tag = "Foo"))).isTrue()
        expectThat(matches("""tag:"foo"""", entry(tag = "FooBar"))).isFalse()
    }

    @Test
    fun `should treat a level as a minimum`() {
        expectThat(matches("level:W", entry(level = LogLevel.WARN))).isTrue()
        expectThat(matches("level:W", entry(level = LogLevel.ERROR))).isTrue()
        expectThat(matches("level:W", entry(level = LogLevel.INFO))).isFalse()
    }

    @Test
    fun `should resolve every spelling of a level`() {
        listOf("level:E", "level:e", "level:err", "level:ERROR").forEach { query ->
            expectThat(matches(query, entry(level = LogLevel.ERROR))).isTrue()
            expectThat(matches(query, entry(level = LogLevel.WARN))).isFalse()
        }
    }

    @Test
    fun `should compare pid numerically`() {
        expectThat(matches("pid:1234", entry(pid = 1234))).isTrue()
        expectThat(matches("pid:1234", entry(pid = 9999))).isFalse()
    }

    @Test
    fun `should match nothing for a non-numeric pid`() {
        expectThat(matches("pid:abc", entry(pid = 1234))).isFalse()
    }

    @Test
    fun `should scope package mine to this process`() {
        expectThat(matches("package:mine", entry(pid = 1234))).isTrue()
        expectThat(matches("package:mine", entry(pid = 9999))).isFalse()
    }

    @Test
    fun `should match a package literal only against this app`() {
        expectThat(matches("package:flixclusive", entry(pid = 1234))).isTrue()
        expectThat(matches("package:com.other", entry(pid = 1234))).isFalse()
        expectThat(matches("package:flixclusive", entry(pid = 9999))).isFalse()
    }

    @Test
    fun `should invert a negated term`() {
        expectThat(matches("-tag:foo", entry(tag = "Foo"))).isFalse()
        expectThat(matches("-tag:foo", entry(tag = "Bar"))).isTrue()
        expectThat(matches("-level:W", entry(level = LogLevel.INFO))).isTrue()
    }

    @Test
    fun `should require every term to match`() {
        val target = entry(tag = "Player", level = LogLevel.ERROR, message = "stream failed")

        expectThat(matches("tag:Player level:E stream", target)).isTrue()
        expectThat(matches("tag:Player level:E missing", target)).isFalse()
    }

    @Test
    fun `should let a bare term hit either the tag or the message`() {
        expectThat(matches("Player", entry(tag = "Player", message = "body"))).isTrue()
        expectThat(matches("body", entry(tag = "Player", message = "body"))).isTrue()
        expectThat(matches("absent", entry(tag = "Player", message = "body"))).isFalse()
    }

    @Test
    fun `should match a regex term`() {
        expectThat(matches("tag~:^Pl.*er$", entry(tag = "Player"))).isTrue()
        expectThat(matches("tag~:^Pl.*er$", entry(tag = "Provider"))).isFalse()
    }

    @Test
    fun `should match everything for an empty query`() {
        expectThat(matches("", entry())).isTrue()
    }
}
