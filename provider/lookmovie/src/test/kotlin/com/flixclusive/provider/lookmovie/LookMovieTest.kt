package com.flixclusive.provider.lookmovie

import com.flixclusive.provider.base.testing.BaseProviderTest
import okhttp3.OkHttpClient
import org.junit.Before

class LookMovieTest : BaseProviderTest() {

    @Before
    override fun setUp() {
        super.setUp()

        sourceProvider = LookMovie(OkHttpClient())
    }
}