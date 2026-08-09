package com.flixclusive.core.database.entity.media

import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toMediaMetadata
import com.flixclusive.model.media.PartialMedia
import com.flixclusive.model.media.common.MediaType
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class DBMediaTest {
    private fun media(releaseDate: Long?) = PartialMedia(
        id = "media-1",
        title = "Test Movie",
        providerId = "test-provider",
        type = MediaType.MOVIE,
        posterImage = null,
        releaseDate = releaseDate,
    )

    private fun storedMillisOf(releaseDate: Long?) = media(releaseDate).toDBMedia().releaseDate?.time

    @Test
    fun `a release date in seconds is stored as milliseconds`() {
        expectThat(storedMillisOf(SECONDS_2010)).isEqualTo(MILLIS_2010)
    }

    @Test
    fun `a release date already in milliseconds is stored unchanged`() {
        expectThat(storedMillisOf(MILLIS_2010)).isEqualTo(MILLIS_2010)
    }

    /**
     * The case a fixed "below a trillion means seconds" cutoff got wrong: milliseconds for anything
     * before September 2001 sit below that cutoff too, so they were read as seconds and multiplied
     * by a thousand.
     */
    @Test
    fun `milliseconds from before 2001 are not mistaken for seconds`() {
        expectThat(storedMillisOf(MILLIS_1980)).isEqualTo(MILLIS_1980)
        expectThat(storedMillisOf(MILLIS_1950)).isEqualTo(MILLIS_1950)
    }

    @Test
    fun `seconds from before 1970 are still scaled up`() {
        expectThat(storedMillisOf(MILLIS_1950 / 1_000)).isEqualTo(MILLIS_1950)
    }

    /**
     * Reading a DBMedia back hands `Date.time` — milliseconds — straight into toDBMedia again, so
     * anything the conversion does has to be idempotent or the value drifts by a factor of a
     * thousand per save.
     */
    @Test
    fun `saving and reading repeatedly does not drift`() {
        for (millis in listOf(MILLIS_2010, MILLIS_1980, MILLIS_1950)) {
            var stored = media(millis).toDBMedia()

            repeat(5) {
                stored = stored.toMediaMetadata(emptyMap()).toDBMedia()
                expectThat(stored.releaseDate?.time).isEqualTo(millis)
            }
        }
    }

    @Test
    fun `an absent release date is stored as null rather than 1970`() {
        expectThat(storedMillisOf(null)).isNull()
        expectThat(storedMillisOf(0L)).isNull()
    }

    @Test
    fun `an absent release date survives a round trip as null`() {
        val stored = media(null).toDBMedia()

        expectThat(stored.toMediaMetadata(emptyMap()).toDBMedia().releaseDate).isNull()
    }

    /** What the pre-Schema23to24 rows hold: the real timestamp multiplied by a thousand. */
    @Test
    fun `a value stored as microseconds is scaled back down`() {
        expectThat(storedMillisOf(MILLIS_2010 * 1_000)).isEqualTo(MILLIS_2010)
    }

    @Test
    fun `a value no reading can rescue is dropped rather than stored`() {
        expectThat(storedMillisOf(Long.MAX_VALUE)).isNull()
        expectThat(storedMillisOf(Long.MIN_VALUE)).isNull()
    }

    private companion object {
        /** 6 Sep 2010 — comfortably past the old cutoff. */
        const val MILLIS_2010 = 1_283_731_200_000L
        const val SECONDS_2010 = MILLIS_2010 / 1_000

        /** 1 Jan 1980 — milliseconds, but small enough that the old cutoff read them as seconds. */
        const val MILLIS_1980 = 315_532_800_000L

        /** 1 Jan 1950 — negative, the same trap on the other side of the epoch. */
        const val MILLIS_1950 = -631_152_000_000L
    }
}
