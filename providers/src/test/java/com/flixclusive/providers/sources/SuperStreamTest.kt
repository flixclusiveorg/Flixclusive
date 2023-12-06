package com.flixclusive.providers.sources

import com.flixclusive.providers.BaseSourceProviderTest
import com.flixclusive.providers.models.common.MediaType
import com.flixclusive.providers.sources.superstream.SuperStream
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class SuperStreamTest : BaseSourceProviderTest() {

    @Before
    override fun setUp() {
        super.setUp()

        sourceProvider = SuperStream(OkHttpClient())
    }

    @Test
    fun `Search for The Dark Knight (2008)`() = runTest {
        val response = sourceProvider.search("The Dark Knight")

        assert(response.results.isNotEmpty())
        assert(response.results.any { it.releaseDate == "2008" })
    }

    @Test
    fun `Get The Dark Knight (2008) source`() = runTest {
        val response = sourceProvider.search("The Dark Knight")

        val theDarkKnight = response.results.find { it.releaseDate == "2008" }
        assertNotNull(theDarkKnight?.id)

        val data = sourceProvider.getSourceLinks(theDarkKnight!!.id!!)

        assert(data.source.isNotBlank())
        assert(data.subtitles.isNotEmpty())
        assert(data.servers?.isNotEmpty() == true)
        assert(data.subtitles.isNotEmpty())
    }

    @Test
    fun `Get Silo (TV-2023) Details`() = runTest {
        val title = "Silo"
        val response = sourceProvider.search(title)

        val silo = response.results.find { it.releaseDate == "2023" }
        assertNotNull(silo?.id)

        val data = sourceProvider.getMediaInfo(silo!!.id!!, MediaType.TvShow)

        assertEquals(data.title, title)
        assert(data.releaseDate.isNotBlank())
        assert(data.id.isNotEmpty())
    }

    @Test
    fun `Get Attack on Titan (TV) S1-E3`() = runTest {
        val title = "Attack on Titan"
        val season = 1
        val episode = 3

        val response = sourceProvider.search(title)

        val aOt = response.results.find { it.releaseDate == "2013" }
        assertNotNull(aOt?.id)

        val data = sourceProvider.getSourceLinks(aOt!!.id!!, season = season, episode = episode)

        assert(data.subtitles.isNotEmpty())
        assert(data.servers?.isNotEmpty() == true)
        assertEquals(data.mediaId, "168")
    }
}