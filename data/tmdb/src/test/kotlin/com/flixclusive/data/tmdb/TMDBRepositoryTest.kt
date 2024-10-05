package com.flixclusive.data.tmdb

import com.flixclusive.core.network.util.Resource
import com.flixclusive.core.util.log.LogRule
import com.flixclusive.core.util.log.debugLog
import com.flixclusive.data.configuration.AppConfigurationManager
import com.flixclusive.data.configuration.di.test.TestAppConfigurationModule.getMockAppConfigurationManager
import com.flixclusive.data.tmdb.di.TestTmdbDataModule.getMockTMDBRepository
import com.flixclusive.model.film.FilmDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TMDBRepositoryTest {

    private val testScope = TestScope(UnconfinedTestDispatcher())

    @get:Rule
    val logRule: LogRule = LogRule()

    private lateinit var tmdbRepository: TMDBRepository
    private lateinit var appConfigurationManager: AppConfigurationManager

    @Before
    fun setup() {
        appConfigurationManager = getMockAppConfigurationManager()
        tmdbRepository = getMockTMDBRepository()
    }

    @Test
    fun `fetch all films from categories`() = testScope.runTest {
        val homeCategories = appConfigurationManager.homeCatalogsData?.run {
            movie + tv + all
        }

        val searchCategories = appConfigurationManager.searchCatalogsData?.run {
            type + genres + companies + networks
        }

        homeCategories?.forEach {
            Assert.assertNotNull(it.url)
            val response = tmdbRepository.paginateConfigItems(
                url = it.url,
                page = 1
            )

            Assert.assertTrue(response is Resource.Success)
            Assert.assertNotNull(response.data)
            debugLog("☑\uFE0F ${it.name}")
        }

        searchCategories?.forEach {
            Assert.assertNotNull(it.url)
            val response = tmdbRepository.paginateConfigItems(
                url = it.url,
                page = 1
            )

            Assert.assertTrue(response is Resource.Success)
            Assert.assertNotNull(response.data)
            debugLog("☑\uFE0F ${it.name}")
        }
    }

    private  fun Resource<FilmDetails>.verifyFilmDetails() {
        Assert.assertTrue(this is Resource.Success)
        Assert.assertNotNull(data)

        data?.run {
            Assert.assertNotNull(title)
            debugLog("☑\uFE0F $title [$providerName]")
            Assert.assertNotNull(tmdbId)
            debugLog("\t > $tmdbId")
            Assert.assertNotNull(year)
            debugLog("\t > $year")
            Assert.assertNotNull(imdbId)
            debugLog("\t > $imdbId")
            Assert.assertNotNull(cast.firstOrNull())
            debugLog("\t > ${cast.first()}")
            Assert.assertNotNull(tagLine)
            debugLog("\t > $tagLine")
            Assert.assertNotNull(runtime)
            debugLog("\t > $runtime")
            Assert.assertNotNull(logoImage)
            debugLog("\t > $logoImage")
        }
    }

    @Test
    fun `Get Avengers Endgame Movie` () = testScope.runTest {
        val movieId = 299534 // Avengers: Endgame
        val response = tmdbRepository.getMovie(id = movieId)
        response.verifyFilmDetails()
    }

    @Test
    fun `Get Arcane TV Show` () = testScope.runTest {
        val tvShowId = 94605 // Arcane
        val response = tmdbRepository.getTvShow(id = tvShowId)
        response.verifyFilmDetails()
    }

    @Test
    fun `Get Watch Providers of Arcane TV Show` () = testScope.runTest {
        val tvShowId = 94605 // Arcane
        val response = tmdbRepository.getWatchProviders(mediaType = "tv", id = tvShowId)

        assert(response is Resource.Success)
        assert(response.data?.isNotEmpty() == true)
        debugLog("☑\uFE0F ${response.data}")
    }
}