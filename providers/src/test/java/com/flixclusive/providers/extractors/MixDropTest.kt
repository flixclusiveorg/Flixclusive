package com.flixclusive.providers.extractors

import com.flixclusive.providers.extractors.mixdrop.MixDrop
import okhttp3.OkHttpClient
import org.junit.Before

class MixDropTest {
    lateinit var mixDrop: MixDrop

    @Before
    fun setUp() {
        mixDrop = MixDrop(OkHttpClient())
    }

    //@Test
    //fun `Extraction test`() = runTest {
    //    val testUrl = "https://mixdrop.co/e/zp7gv9wehwlq60"
    //    val result = mixDrop.extract(
    //        url = URL(testUrl),
    //        mediaId = "",
    //        episodeId = ""
    //    )
    //
    //    assert(result.servers.isNotEmpty())
    //}
}