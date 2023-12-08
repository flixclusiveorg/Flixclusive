package com.flixclusive.providers.sources

import com.flixclusive.providers.BaseSourceProviderTest
import com.flixclusive.providers.sources.lookmovie.LookMovie
import okhttp3.OkHttpClient
import org.junit.Before

class LookMovieTest : BaseSourceProviderTest() {

    @Before
    override fun setUp() {
        super.setUp()

        sourceProvider = LookMovie(OkHttpClient())
    }
}