package com.flixclusive.providers

import android.util.Base64
import com.flixclusive.providers.interfaces.SourceProvider
import com.flixclusive.providers.models.common.MediaType
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

abstract class BaseSourceProviderTest {
    lateinit var sourceProvider: SourceProvider

    @Before
    open fun setUp(): Unit = runTest {
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            val byteArray = arg<ByteArray>(0)
            java.util.Base64.getEncoder().encodeToString(byteArray)
        }
        every { Base64.encode(any(), any()) } answers {
            val byteArray = arg<String>(0).toByteArray()
            java.util.Base64.getEncoder().encode(byteArray)
        }
        every { Base64.decode(any<String>(), any()) } answers {
            val byteArray = arg<String>(0).toByteArray()
            java.util.Base64.getDecoder().decode(byteArray)
        }
        every { Base64.decode(any<ByteArray>(), any()) } answers {
            java.util.Base64.getDecoder().decode(arg<ByteArray>(0))
        }
    }

    @Test
    open fun `Search for The Dark Knight (2008)`() = runTest {
        val title = "The Dark Knight"
        val releaseDate = "2008"
        val response = sourceProvider.search(
            query = title,
            page = 1,
            mediaType = MediaType.Movie
        )

        assert(response.results.isNotEmpty())
        assert(response.results.any { it.releaseDate == releaseDate })
    }

    @Test
    open fun `Get The Dark Knight (2008) source`() = runTest {
        val title = "The Dark Knight"
        val releaseDate = "2008"
        val response = sourceProvider.search(
            query = title,
            page = 1,
            mediaType = MediaType.Movie
        )

        val theDarkKnight = response.results.find { it.releaseDate == releaseDate }
        Assert.assertNotNull(theDarkKnight?.id)

        val data = sourceProvider.getSourceLinks(theDarkKnight!!.id!!)

        assert(data.source.isNotBlank())
        assert(data.subtitles.isNotEmpty())
        assert(data.servers?.isNotEmpty() == true)
        assert(data.subtitles.isNotEmpty())
    }

    @Test
    open fun `Get World War Z (2013) source`() = runTest {
        val title = "World War Z"
        val releaseDate = "2013"
        val response = sourceProvider.search(
            query = title,
            page = 1,
            mediaType = MediaType.Movie
        )

        val fnaf = response.results.find { it.releaseDate == releaseDate }
        Assert.assertNotNull(fnaf?.id)

        val data = sourceProvider.getSourceLinks(fnaf!!.id!!)

        assert(data.source.isNotBlank())
        assert(data.subtitles.isNotEmpty())
        assert(data.servers?.isNotEmpty() == true)
        assert(data.subtitles.isNotEmpty())
    }

    @Test
    open fun `Get Silo (TV-2023) Details`() = runTest {
        val title = "Silo"
        val releaseDate = "2023"
        val response = sourceProvider.search(
            query = title,
            page = 1,
            mediaType = MediaType.TvShow
        )

        val silo = response.results.find {
            val releaseDateToUse = if(it.releaseDate == null) {
                sourceProvider.getMediaInfo(it.id!!, it.mediaType!!).releaseDate
            } else it.releaseDate

            releaseDateToUse == releaseDate
        }
        Assert.assertNotNull(silo?.id)

        val data = sourceProvider.getMediaInfo(silo!!.id!!, MediaType.TvShow)

        Assert.assertEquals(data.title, title)
        assert(data.releaseDate.isNotBlank())
        assert(data.id.isNotEmpty())
    }

    @Test
    open fun `Get Attack on Titan (TV) S1-E3`() = runTest {
        val title = "Attack on Titan"
        val releaseDate = "2013"
        val season = 1
        val episode = 3

        val response = sourceProvider.search(
            query = title,
            page = 1,
            mediaType = MediaType.TvShow
        )

        val aOt = response.results.find {
            val releaseDateToUse = if(it.releaseDate == null) {
                sourceProvider.getMediaInfo(it.id!!, it.mediaType!!).releaseDate
            } else it.releaseDate

            releaseDateToUse == releaseDate
        }
        Assert.assertNotNull(aOt?.id)

        val data = sourceProvider.getSourceLinks(aOt!!.id!!, season = season, episode = episode)

        assert(data.subtitles.isNotEmpty())
        assert(data.servers?.isNotEmpty() == true)
        assert(data.mediaId.isNotEmpty())
    }
}