package com.flixclusive.provider.superstream

import com.flixclusive.provider.base.testing.BaseProviderTest
import okhttp3.OkHttpClient
import org.junit.Before

class SuperStreamTest : BaseProviderTest() {

    @Before
    override fun setUp() {
        super.setUp()

        sourceProvider = SuperStream(OkHttpClient())
    }
}