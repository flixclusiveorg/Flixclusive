package com.flixclusive.domain.provider.usecase.get.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.common.domain.Async
import com.flixclusive.core.common.provider.LoadLinksState
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.provider.CachedStream
import com.flixclusive.core.database.entity.provider.CachedSubtitle
import com.flixclusive.core.database.entity.provider.MediaLinksWithData
import com.flixclusive.core.datastore.DataStoreManager
import com.flixclusive.core.datastore.UserSessionDataStore
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.UserPreferences
import com.flixclusive.data.provider.repository.MediaLinksRepository
import com.flixclusive.domain.provider.usecase.get.GetMediaLinksUseCase
import com.flixclusive.model.media.Movie
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

/**
 * Instrumented, not a JVM unit test: [SubtitlesPreferences]'s default [android.graphics.Color]/
 * [android.graphics.Typeface]-backed fields touch real Android framework classes at construction
 * time, which throw under plain JVM unit tests since this project has no Robolectric — same
 * reasoning as [com.flixclusive.data.downloads.hls.impl.HlsManifestResolverImplTest].
 */
@RunWith(AndroidJUnit4::class)
class GetDownloadableMediaLinksUseCaseImplTest {
    private lateinit var getMediaLinksUseCase: GetMediaLinksUseCase
    private lateinit var mediaLinksRepository: MediaLinksRepository
    private lateinit var userSessionDataStore: UserSessionDataStore
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var useCase: GetDownloadableMediaLinksUseCaseImpl

    private val testMovie = Movie(
        id = "123",
        title = "Test Movie",
        providerId = "test-provider",
        posterImage = null,
    )

    @Before
    fun setup() {
        getMediaLinksUseCase = mockk()
        mediaLinksRepository = mockk()
        userSessionDataStore = mockk()
        dataStoreManager = mockk()

        every { userSessionDataStore.currentUserId } returns flowOf("user-1")
        every {
            dataStoreManager.getUserPrefsAsFlow(UserPreferences.SUBTITLES_PREFS_KEY, SubtitlesPreferences::class)
        } returns flowOf(SubtitlesPreferences(subtitleLanguage = "en"))

        useCase = GetDownloadableMediaLinksUseCaseImpl(
            getMediaLinksUseCase = getMediaLinksUseCase,
            mediaLinksRepository = mediaLinksRepository,
            userSessionDataStore = userSessionDataStore,
            dataStoreManager = dataStoreManager,
        )
    }

    private fun cachedStream(url: String, isDead: Boolean = false) = CachedStream(
        url = url,
        label = "1080p",
        providerId = "test-provider",
        ownerId = "user-1",
        mediaId = testMovie.id,
        isDead = isDead,
    )

    private fun cachedSubtitle(url: String, label: String) = CachedSubtitle(
        url = url,
        label = label,
        providerId = "test-provider",
        ownerId = "user-1",
        mediaId = testMovie.id,
    )

    @Test
    fun invokeShouldReturnFailureWhenLinkResolutionDoesNotSucceed() =
        runTest {
            coEvery { getMediaLinksUseCase(testMovie, null) } returns flowOf(LoadLinksState.Unavailable())

            val result = useCase(testMovie, null)

            expectThat(result).isA<Async.Failure>()
        }

    @Test
    fun invokeShouldReturnFailureWhenNoValidStreamsAreCached() =
        runTest {
            coEvery { getMediaLinksUseCase(testMovie, null) } returns flowOf(LoadLinksState.Success)
            coEvery { mediaLinksRepository.getLinks("user-1", testMovie.id, null, null) } returns listOf(
                MediaLinksWithData(
                    media = testMovie.toDBMedia(),
                    streams = listOf(cachedStream("https://example.com/dead.mp4", isDead = true)),
                ),
            )

            val result = useCase(testMovie, null)

            expectThat(result).isA<Async.Failure>()
        }

    @Test
    fun invokeShouldMapCachedStreamsAndPickSubtitleMatchingPreferredLanguage() =
        runTest {
            coEvery { getMediaLinksUseCase(testMovie, null) } returns flowOf(LoadLinksState.Success)
            coEvery { mediaLinksRepository.getLinks("user-1", testMovie.id, null, null) } returns listOf(
                MediaLinksWithData(
                    media = testMovie.toDBMedia(),
                    streams = listOf(cachedStream("https://example.com/stream.mp4")),
                    subtitles = listOf(
                        cachedSubtitle("https://example.com/fr.srt", label = "fr"),
                        cachedSubtitle("https://example.com/en.srt", label = "en"),
                    ),
                ),
            )

            val result = useCase(testMovie, null)

            expectThat(result).isA<Async.Success<*>>()
            val data = (result as Async.Success).data
            expectThat(data.streams.map { it.url }).isEqualTo(listOf("https://example.com/stream.mp4"))
            expectThat(data.subtitle?.url).isEqualTo("https://example.com/en.srt")
        }

    @Test
    fun invokeShouldFallBackToFirstSubtitleWhenNoneMatchPreferredLanguage() =
        runTest {
            coEvery { getMediaLinksUseCase(testMovie, null) } returns flowOf(LoadLinksState.Success)
            coEvery { mediaLinksRepository.getLinks("user-1", testMovie.id, null, null) } returns listOf(
                MediaLinksWithData(
                    media = testMovie.toDBMedia(),
                    streams = listOf(cachedStream("https://example.com/stream.mp4")),
                    subtitles = listOf(cachedSubtitle("https://example.com/fr.srt", label = "fr")),
                ),
            )

            val result = useCase(testMovie, null)

            val data = (result as Async.Success).data
            expectThat(data.subtitle?.url).isEqualTo("https://example.com/fr.srt")
        }

    @Test
    fun invokeShouldReturnNullSubtitleWhenNoneAreCached() =
        runTest {
            coEvery { getMediaLinksUseCase(testMovie, null) } returns flowOf(LoadLinksState.Success)
            coEvery { mediaLinksRepository.getLinks("user-1", testMovie.id, null, null) } returns listOf(
                MediaLinksWithData(
                    media = testMovie.toDBMedia(),
                    streams = listOf(cachedStream("https://example.com/stream.mp4")),
                ),
            )

            val result = useCase(testMovie, null)

            val data = (result as Async.Success).data
            expectThat(data.subtitle).isNull()
        }
}
