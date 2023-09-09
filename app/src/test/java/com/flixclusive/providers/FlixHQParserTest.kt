package com.flixclusive.providers

import com.flixclusive_provider.interfaces.FilmSourcesProvider
import com.flixclusive_provider.providers.flixhq.FlixHQ
import com.flixclusive_provider.utils.DecryptUtils.decryptAes
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FlixHQParserTest {
    companion object {
        private lateinit var filmSourcesProvider: FilmSourcesProvider

        @BeforeAll
        @JvmStatic
        fun setUp() {
            filmSourcesProvider = FlixHQ(OkHttpClient())
        }
    }

    @Test
    fun flixHQ_SearchForSilo_Returns2Items() {
        val result = filmSourcesProvider.search("Silo")
        assertEquals(2, result.results.size)
    }

    @Test
    fun flixHQ_GetTheFlashAvailableServers_Returns2ServersWithVidCloudBeingFirst() {
        val result = filmSourcesProvider.getAvailableServers("2899", "tv/watch-the-flash-39535")
        assertEquals(3, result.size)
        assertEquals("vidcloud", result[1].serverName)
    }

    @Test
    fun flixHQ_GetFromAvailableSources_Returns2Items() {
        val result = filmSourcesProvider.getStreamingLinks("1350103", "tv/watch-from-77455")
        assertNotEquals("", result.source)
    }

    @Test
    fun flixHQ_Decryption_ReturnsTrue() {
        val encString = "U2FsdGVkX19CIuaIRgRRf1pJVxKY/7n1obeNUcQrizcCUdaPwDa7OG3pNT7KDRD7DfFIlUha8IPbVvmtYsY+1ehAGVp3mV5KtcVo+8AfsoqWkSkP7KuCTggiymPOQg094fWDy4pHAUGHe+RmO9ZQi0SE+MWrDbKTeAhtC4TlLVpHJMFYqw1rlNdAD7rRp6H5HCsnG8QIZ1QqQN8G3nnYd5BYbYkvvsYT+ahTlY2r1C5CkDzyaeb647VVjRsYBSTAIaCVk3xC/seXA3dQgICoVWPHXPgVSMj0USc8Dirdacc0U3HL0ySkZPtxC8mhhr1bXnnHCN10SACWEJpZwDXhyPE6pwer0FnySnUW/eqVGiCpZxdvNjWIuvGBqtyNUIm1cpzQsxRfVreq7YwP1BDFU2OwYPtFq7DY4KPqbZtLGSAHRL0OcmnucVuxRRRUIn5PPxdL2my26YjNZHUuzgySOsMuvpp1LwpXk2HdyZWOJmft0JwdKgj4RDz5yKDbGXIF"
        val key = "ZTwVpfLANX3t6Xb7cBpbkmFrGT0bCpEvBiN"

        val result = decryptAes(encString, key)
        assertEquals(true, result.contains("http"))
    }
}