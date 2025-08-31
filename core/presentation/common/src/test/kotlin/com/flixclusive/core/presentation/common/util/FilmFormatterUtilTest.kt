package com.flixclusive.core.presentation.common.util

import android.content.Context
import com.flixclusive.core.presentation.common.R
import com.flixclusive.core.presentation.common.util.FilmFormatterUtil.formatAsRating
import com.flixclusive.core.presentation.common.util.FilmFormatterUtil.formatAsRuntime
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.Locale


class FilmFormatterUtilTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk {
            every { getString(R.string.no_runtime, *anyVararg<Any>()) } returns "No runtime"
            every { getString(R.string.no_ratings, *anyVararg<Any>()) } returns "No ratings"
            every {
                resources.getQuantityString(
                    R.plurals.season_runtime,
                    any(),
                    any()
                )
            } answers {
                val count = args[1] as Int
                val string = String.format(Locale.ROOT, "%d Season", count)

                string + if (count > 1) "s" else ""
            }
            every {
                resources.getQuantityString(
                    R.plurals.episode_runtime,
                    any(),
                    any()
                )
            } answers {
                val count = args[1] as Int
                val string = String.format(Locale.ROOT, "%d Episode", count)

                string + if (count > 1) "s" else ""
            }
        }
    }

    @Test
    fun formatAsRuntime() {
        val testCases = listOf(
            0 to "No runtime",
            45 to "45m",
            120 to "2h",
            135 to "2h 15m",
            60 to "1h",
            61 to "1h 1m",
            59 to "59m",
            -10 to "No runtime"
        )

        for ((input, expected) in testCases) {
            val result = input.formatAsRuntime()
            expectThat(result.asString(context))
                .isEqualTo(expected)
        }
    }

    @Test
    fun formatAsRating() {
        val testCases = listOf(
            0.0 to "No ratings",
            3.0 to "3.0",
            4.25 to "4.25",
            4.2 to "4.2",
            5.75 to "5.75",
            2.5 to "2.5",
            4.567 to "4.57"
        )

        for ((input, expected) in testCases) {
            val result = input.formatAsRating()
            expectThat(result.asString(context))
                .isEqualTo(expected)
        }
    }

    @Test
    fun formatTvRuntime() {
        val testCases = listOf(
            Triple(45, 3, 24) to "45m | 3 Seasons | 24 Episodes | ",
            Triple(null, 1, 10) to "1 Season | 10 Episodes | ",
            Triple(30, 0, 0) to "30m | ",
            Triple(60, 2, 0) to "1h | 2 Seasons | ",
            Triple(null, 0, 5) to "5 Episodes | ",
            Triple(null, 0, 1) to "1 Episode | ",
            Triple(90, 1, 1) to "1h 30m | 1 Season | 1 Episode | "
        )

        for ((input, expected) in testCases) {
            val (minutesPerEpisode, seasons, episodes) = input
            val result = FilmFormatterUtil.formatTvRuntime(
                context = context,
                minutesPerEpisode = minutesPerEpisode,
                seasons = seasons,
                episodes = episodes
            )
            expectThat(result)
                .isEqualTo(expected)
        }
    }
}
