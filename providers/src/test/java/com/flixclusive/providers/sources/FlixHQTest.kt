package com.flixclusive.providers.sources

import com.flixclusive.providers.BaseSourceProviderTest
import com.flixclusive.providers.sources.flixhq.FlixHQ
import com.flixclusive.providers.utils.DecryptUtils.decryptAes
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@Suppress("SpellCheckingInspection")
class FlixHQTest : BaseSourceProviderTest() {

    @Before
    override fun setUp() {
        super.setUp()

        sourceProvider = FlixHQ(OkHttpClient())
    }

    @Test
    fun `Silo (tv show) search should return two items`() = runTest {
        val result = sourceProvider.search("Silo")
        assertEquals(2, result.results.size)
    }

    @Test
    fun `The Flash (tv show) available servers should be more than 2`() = runTest {
        val season = 1
        val episode = 2

        val result = sourceProvider.getAvailableServers(
            mediaId = "tv/watch-the-flash-39535",
            season = season,
            episode = episode
        )
        assert(2 >= result.size)
        assertEquals("vidcloud", result[1].serverName)
    }

    @Test
    fun `From (tv show) test default (UpCloud) source`() = runTest {
        val result = sourceProvider.getSourceLinks(
            mediaId = "tv/watch-from-77455",
            episode = 1,
            season = 1
        )
        assert(result.source.isNotEmpty())
    }

    @Test
    fun `The Dark Knight (movie) test UpCloud source`() = runTest {
        val result = sourceProvider.getSourceLinks(
            mediaId = "movie/watch-the-dark-knight-19752",
            server = "upcloud"
        )
        assert(result.source.isNotEmpty())
    }

    //@Test
    //fun flixHQ_GetFromAvailableSources_MixDrop() {
    //    val providerServers = sourceProvider.getAvailableServers("19752", "movie/watch-the-dark-knight-19752")
    //    val mixDropServer = providerServers.firstOrNull { it.serverName == MediaServer.MixDrop.serverName }
    //    assert(mixDropServer != null)
    //
    //    val result = sourceProvider.getStreamingLinks("19752", "movie/watch-the-dark-knight-19752", server = mixDropServer!!)
    //    assert(result.source.isNotEmpty())
    //}

    @Test
    fun `verify decryption tool`() {
        val encString = "U2FsdGVkX19CIuaIRgRRf1pJVxKY/7n1obeNUcQrizcCUdaPwDa7OG3pNT7KDRD7DfFIlUha8IPbVvmtYsY+1ehAGVp3mV5KtcVo+8AfsoqWkSkP7KuCTggiymPOQg094fWDy4pHAUGHe+RmO9ZQi0SE+MWrDbKTeAhtC4TlLVpHJMFYqw1rlNdAD7rRp6H5HCsnG8QIZ1QqQN8G3nnYd5BYbYkvvsYT+ahTlY2r1C5CkDzyaeb647VVjRsYBSTAIaCVk3xC/seXA3dQgICoVWPHXPgVSMj0USc8Dirdacc0U3HL0ySkZPtxC8mhhr1bXnnHCN10SACWEJpZwDXhyPE6pwer0FnySnUW/eqVGiCpZxdvNjWIuvGBqtyNUIm1cpzQsxRfVreq7YwP1BDFU2OwYPtFq7DY4KPqbZtLGSAHRL0OcmnucVuxRRRUIn5PPxdL2my26YjNZHUuzgySOsMuvpp1LwpXk2HdyZWOJmft0JwdKgj4RDz5yKDbGXIF"
        val key = "ZTwVpfLANX3t6Xb7cBpbkmFrGT0bCpEvBiN"

        val result = decryptAes(encString, key)
        assertEquals(true, result.contains("http"))
    }
}