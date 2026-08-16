package com.flixclusive.feature.mobile.settings.screen.system.logcat

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class LogFilterParserTest {
    private fun parse(query: String) = LogFilterParser.parse(query)

    @Test
    fun `should treat a bare word as a substring term`() {
        expectThat(parse("timeout").filter)
            .isA<LogFilter.Bare>()
            .get { matcher }
            .isEqualTo(ValueMatcher.Substring("timeout"))
    }

    @Test
    fun `should parse an unquoted key as a substring field`() {
        expectThat(parse("tag:Foo").filter)
            .isA<LogFilter.Field>()
            .and {
                get { key }.isEqualTo(FilterKey.TAG)
                get { matcher }.isEqualTo(ValueMatcher.Substring("Foo"))
            }
    }

    @Test
    fun `should parse a quoted value as exact and keep its spaces`() {
        expectThat(parse("""tag:"My Tag"""").filter)
            .isA<LogFilter.Field>()
            .get { matcher }
            .isEqualTo(ValueMatcher.Exact("My Tag"))
    }

    @Test
    fun `should parse the regex operator`() {
        expectThat(parse("tag~:F.*").filter)
            .isA<LogFilter.Field>()
            .get { matcher }
            .isA<ValueMatcher.Pattern>()
    }

    @Test
    fun `should parse negation`() {
        expectThat(parse("-tag:Foo").filter)
            .isA<LogFilter.Not>()
            .get { term }
            .isA<LogFilter.Field>()
            .get { key }
            .isEqualTo(FilterKey.TAG)
    }

    @Test
    fun `should and multiple terms in order`() {
        expectThat(parse("tag:Foo level:E timeout").filter)
            .isA<LogFilter.And>()
            .get { terms }
            .hasSize(3)
    }

    @Test
    fun `should alias msg to message`() {
        expectThat(parse("msg:body").filter)
            .isA<LogFilter.Field>()
            .get { key }
            .isEqualTo(FilterKey.MESSAGE)
    }

    @Test
    fun `should keep an unknown key as a bare term`() {
        expectThat(parse("foo:bar").filter)
            .isA<LogFilter.Bare>()
            .get { matcher }
            .isEqualTo(ValueMatcher.Substring("foo:bar"))
    }

    @Test
    fun `should match everything for an empty query`() {
        expectThat(parse("").filter).isA<LogFilter.MatchAll>()
        expectThat(parse("   ").filter).isA<LogFilter.MatchAll>()
    }

    @Test
    fun `should report an unterminated quote without dropping the term`() {
        val parsed = parse("""tag:"My Tag""")

        expectThat(parsed.error).isNotNull()
        expectThat(parsed.filter).isA<LogFilter.Field>().get { key }.isEqualTo(FilterKey.TAG)
    }

    @Test
    fun `should degrade an invalid regex to a substring term`() {
        val parsed = parse("msg~:[")

        expectThat(parsed.error).isNotNull()
        expectThat(parsed.filter)
            .isA<LogFilter.Field>()
            .get { matcher }
            .isEqualTo(ValueMatcher.Substring("["))
    }

    @Test
    fun `should unescape a quote inside a quoted value`() {
        expectThat(parse("""msg:"say \"hi\""""").filter)
            .isA<LogFilter.Field>()
            .get { matcher }
            .isEqualTo(ValueMatcher.Exact("""say "hi""""))
    }

    @Test
    fun `should report no error for a well formed query`() {
        expectThat(parse("package:mine level:W -tag:OkHttp").error).isNull()
    }
}
