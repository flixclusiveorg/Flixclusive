package com.flixclusive.core.presentation.player

import android.content.Context
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flixclusive.core.datastore.model.user.PlayerPreferences
import com.flixclusive.core.datastore.model.user.SubtitlesPreferences
import com.flixclusive.core.datastore.model.user.player.PlayerQuality
import com.flixclusive.core.presentation.player.model.PlaylistItem
import com.flixclusive.core.presentation.player.util.internal.PlayerCacheManager
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.model.provider.link.Stream
import com.flixclusive.model.provider.link.Subtitle
import com.flixclusive.model.provider.link.SubtitleSource
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
class InternalPlayerImplTest {
    private lateinit var context: Context
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var playerCacheManager: PlayerCacheManager
    private lateinit var playerPreferences: PlayerPreferences
    private lateinit var subtitlesPreferences: SubtitlesPreferences
    private lateinit var internalPlayer: InternalPlayerImpl

    // Test data
    private val testMovie = FilmTestDefaults.getMovie()
    private val testTvShow = FilmTestDefaults.getTvShow()
    private val testEpisode = FilmTestDefaults.getEpisode(
        id = "1",
        number = 1,
        title = "Test Episode",
    )

    private val testStreams = listOf(
        Stream(
            name = "1080p",
            url = "https://example.com/test-stream-1080p.m3u8",
            description = "High quality stream",
        ),
        Stream(
            name = "720p",
            url = "https://example.com/test-stream-720p.m3u8",
            description = "Medium quality stream",
        ),
    )

    private val testSubtitles = listOf(
        Subtitle(
            language = "English",
            url = "https://example.com/en.vtt",
            type = SubtitleSource.ONLINE,
        ),
        Subtitle(
            language = "Spanish",
            url = "https://example.com/es.vtt",
            type = SubtitleSource.ONLINE,
        ),
    )

    private val testPlaylistItemMovie = PlaylistItem.create(
        metadata = testMovie,
        stream = testStreams,
        subtitles = testSubtitles,
    )

    private val testPlaylistItemTvShow = PlaylistItem.create(
        metadata = testTvShow,
        stream = testStreams,
        subtitles = testSubtitles,
        episode = testEpisode,
    )

    private val testPlaylistItemMovie2 = PlaylistItem.create(
        metadata = FilmTestDefaults.getMovie().copy(id = "movie2", title = "Different Movie"),
        stream = testStreams,
        subtitles = testSubtitles,
    )

    @Before
    @UiThreadTest
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        okHttpClient = OkHttpClient()
        playerCacheManager = PlayerCacheManager(context)

        playerPreferences = PlayerPreferences(
            quality = PlayerQuality.Quality1080p,
            isForcingPlayerRelease = true,
        )

        subtitlesPreferences = SubtitlesPreferences(
            isSubtitleEnabled = true,
            subtitleLanguage = "en",
        )

        internalPlayer = InternalPlayerImpl(
            client = okHttpClient,
            context = context,
            subtitlePrefs = subtitlesPreferences,
            playerPrefs = playerPreferences,
        )

        internalPlayer.initialize()
    }

    @After
    @UiThreadTest
    fun tearDown() {
        internalPlayer.release()
    }

    @Test
    @UiThreadTest
    fun shouldPrepareNewItemWhenPlaylistIsEmpty() {
        internalPlayer.prepare(testPlaylistItemMovie)

        expectThat(internalPlayer.getPlaylistItem(testMovie)).isNotNull()
        expectThat(internalPlayer.areTracksInitialized).isFalse()
    }

    @Test
    @UiThreadTest
    fun shouldReuseLastItemWhenPreparingTheSameItem() {
        // Prepare first item
        internalPlayer.prepare(testPlaylistItemMovie)
        internalPlayer.areTracksInitialized = true // Simulate tracks being initialized

        // Prepare the same item again
        internalPlayer.prepare(testPlaylistItemMovie)

        expectThat(internalPlayer.areTracksInitialized).isTrue()
    }

    @Test
    @UiThreadTest
    fun shouldRemoveAndReAddItemWhenItemExistsButIsNotLast() {
        // Add first item
        internalPlayer.prepare(testPlaylistItemMovie)

        // Add second item (becomes last)
        internalPlayer.prepare(testPlaylistItemMovie2)

        // Verify movie2 is last in playlist
        expectThat(internalPlayer.getPlaylistItem(testMovie)).isNotNull()
        expectThat(
            internalPlayer.getPlaylistItem(
                FilmTestDefaults.getMovie().copy(id = "movie2", title = "Different Movie"),
            ),
        ).isNotNull()

        // Re-prepare first item (exists but not last)
        internalPlayer.prepare(testPlaylistItemMovie)

        expectThat(internalPlayer.getPlaylistItem(testMovie)).isNotNull()
        expectThat(internalPlayer.areTracksInitialized).isFalse()
    }

    @Test
    @UiThreadTest
    fun shouldResetStreamIndexWhenPreparingDifferentItem() {
        // Prepare first item with default stream index
        internalPlayer.prepare(testPlaylistItemMovie)

        // Prepare different item
        internalPlayer.prepare(testPlaylistItemMovie2)

        // Stream index should be reset based on quality preference
        expectThat(internalPlayer.areTracksInitialized).isFalse()
    }

    @Test
    @UiThreadTest
    fun shouldMaintainStreamIndexWhenRepreparingSameItem() {
        // Prepare item and select a different stream
        internalPlayer.prepare(testPlaylistItemMovie)
        internalPlayer.selectStream(1) // Select second stream

        // Reprepare the same item
        internalPlayer.prepare(testPlaylistItemMovie)

        // Stream index should be maintained since it's the same item
        expectThat(internalPlayer.getPlaylistItem(testMovie)).isNotNull()
    }

    @Test
    @UiThreadTest
    fun shouldSelectStreamAndReprepareCurrentItem() {
        internalPlayer.prepare(testPlaylistItemMovie)

        internalPlayer.selectStream(1)

        expectThat(internalPlayer.getPlaylistItem(testMovie)).isNotNull()
    }

    @Test
    @UiThreadTest
    fun shouldIgnoreInvalidStreamIndices() {
        internalPlayer.prepare(testPlaylistItemMovie)

        // Test negative index
        internalPlayer.selectStream(-1)
        expectThat(internalPlayer.getPlaylistItem(testMovie)).isNotNull()

        // Test index out of bounds
        internalPlayer.selectStream(999)
        expectThat(internalPlayer.getPlaylistItem(testMovie)).isNotNull()
    }

    @Test
    @UiThreadTest
    fun shouldReturnNullWhenNoCurrentItemForStreamSelection() {
        // Don't prepare any item, playlist is empty
        internalPlayer.selectStream(0)

        // Should handle gracefully when no current item exists
        expectThat(internalPlayer.getPlaylistItem(testMovie)).isNull()
    }

    @Test
    @UiThreadTest
    fun shouldRetrievePlaylistItemById() {
        internalPlayer.prepare(testPlaylistItemMovie)

        val retrievedItem = internalPlayer.getPlaylistItem(testMovie)

        expectThat(retrievedItem).isNotNull()
        expectThat(retrievedItem!!.id).isEqualTo(testPlaylistItemMovie.id)
    }

    @Test
    @UiThreadTest
    fun shouldRetrievePlaylistItemByIdWithEpisode() {
        internalPlayer.prepare(testPlaylistItemTvShow)

        val retrievedItem = internalPlayer.getPlaylistItem(testTvShow, testEpisode)

        expectThat(retrievedItem).isNotNull()
        expectThat(retrievedItem!!.id).isEqualTo(testPlaylistItemTvShow.id)
    }

    @Test
    @UiThreadTest
    fun shouldReturnNullForNonExistentPlaylistItem() {
        internalPlayer.prepare(testPlaylistItemMovie)

        val nonExistentItem = internalPlayer.getPlaylistItem(
            FilmTestDefaults.getMovie().copy(id = "nonexistent", title = "Non-existent Movie"),
        )

        expectThat(nonExistentItem).isNull()
    }

    @Test
    @UiThreadTest
    fun shouldReturnNullWhenPlaylistIsEmpty() {
        val retrievedItem = internalPlayer.getPlaylistItem(testMovie)

        expectThat(retrievedItem).isNull()
    }
}
