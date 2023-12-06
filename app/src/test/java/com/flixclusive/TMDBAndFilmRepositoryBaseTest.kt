package com.flixclusive

import android.util.Base64
import android.util.Log
import com.flixclusive.common.Constants
import com.flixclusive.data.api.GithubConfigService
import com.flixclusive.data.api.TMDBApiService
import com.flixclusive.data.config.ConfigurationProviderImpl
import com.flixclusive.data.repository.FilmSourcesRepositoryImpl
import com.flixclusive.data.repository.TMDBRepositoryImpl
import com.flixclusive.domain.common.PaginatedSearchItemsDeserializer
import com.flixclusive.domain.common.Resource
import com.flixclusive.domain.common.SearchItemDeserializer
import com.flixclusive.domain.config.ConfigurationProvider
import com.flixclusive.domain.config.RemoteConfigStatus
import com.flixclusive.domain.model.tmdb.TMDBPageResponse
import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.flixclusive.domain.repository.FilmSourcesRepository
import com.flixclusive.domain.repository.TMDBRepository
import com.google.gson.GsonBuilder
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Before
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

abstract class TMDBAndFilmRepositoryBaseTest {
    private val client = OkHttpClient()

    private lateinit var configurationProvider: ConfigurationProvider
    lateinit var tmdbRepository: TMDBRepository
    lateinit var filmSourcesRepository: FilmSourcesRepository

    lateinit var testScope: TestScope

    // One Piece Data
    //val sampleShowId = 37854
    //val sampleShowSeason = 13
    //val sampleShowEpisode = 423

    val sampleShowId = 1429
    val sampleShowSeason = 2
    val sampleShowEpisode = 3

    // Case Closed Data
    val sampleShowId2 = 30983
    val sampleShowSeason2 = 1
    val sampleShowEpisode2 = 400

    @Before
    open fun setUp(): Unit = runTest {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } answers {
            println(arg<String>(1))
            0
        }

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

        val testDispatcher = StandardTestDispatcher(testScheduler)
        testScope = TestScope(testDispatcher)

        configurationProvider = ConfigurationProviderImpl(
            getGithubApiService(),
            testScope
        )

        configurationProvider.initialize() // Should wait

        while (configurationProvider.remoteStatus.value != RemoteConfigStatus.Success) {
            delay(500)
        }

        filmSourcesRepository =
            FilmSourcesRepositoryImpl(client, tmdbRepository, testDispatcher)

        tmdbRepository = TMDBRepositoryImpl(
            getTMDBApiService(),
            configurationProvider,
            testDispatcher
        )
    }

    private fun getTMDBApiService(): TMDBApiService {
        val tmdbPageResponse = TMDBPageResponse<TMDBSearchItem>()

        val gson = GsonBuilder()
            .registerTypeAdapter(TMDBSearchItem::class.java, SearchItemDeserializer())
            .registerTypeAdapter(tmdbPageResponse::class.java, PaginatedSearchItemsDeserializer())
            .create()

        return Retrofit.Builder()
            .baseUrl(Constants.TMDB_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
            .create(TMDBApiService::class.java)
    }

    private fun getGithubApiService() = Retrofit.Builder()
        .baseUrl(Constants.GITHUB_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()
        .create(GithubConfigService::class.java)

    fun <T> Resource<T>.assertDidNotFailAndNotNull() {
        assert(this !is Resource.Failure)
        Assert.assertNotNull(data)
    }
}