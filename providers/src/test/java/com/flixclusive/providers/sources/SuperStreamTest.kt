package com.flixclusive.providers.sources

import com.flixclusive.providers.BaseSourceProviderTest
import com.flixclusive.providers.sources.superstream.SuperStream
import okhttp3.OkHttpClient
import org.junit.Before

class SuperStreamTest : BaseSourceProviderTest() {

    @Before
    override fun setUp() {
        super.setUp()

        sourceProvider = SuperStream(OkHttpClient())
    }
}