package com.flixclusive.core.testing.database

import android.content.Context
import androidx.room.Room
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.database.entity.media.DBMedia.Companion.toDBMedia
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.database.entity.provider.InstalledRepository
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.testing.media.MediaTestDefaults
import com.flixclusive.model.media.MediaMetadata
import java.util.Date

/**
 * Test defaults for database-related tests.
 * */
object DatabaseTestDefaults {
    const val TEST_USER_ID = "11111111-1111-1111-1111-111111111111"

    fun getDBMedia(media: MediaMetadata = MediaTestDefaults.getMovie()) = media.toDBMedia()

    fun getMovieProgress(
        id: Long = 0,
        mediaId: String = getDBMedia().id,
        ownerId: String = TEST_USER_ID,
        progress: Long = 0,
        status: WatchStatus = WatchStatus.WATCHING,
        duration: Long = 0,
        createdAt: Date = Date(),
        updatedAt: Date = createdAt,
    ) = MovieProgress(
        id = id,
        mediaId = mediaId,
        ownerId = ownerId,
        progress = progress,
        status = status,
        duration = duration,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun getEpisodeProgress(
        id: Long = 0,
        mediaId: String = getDBMedia(media = MediaTestDefaults.getShow()).id,
        ownerId: String = TEST_USER_ID,
        seasonNumber: Int = 1,
        episodeNumber: Int = 1,
        progress: Long = 0,
        status: WatchStatus = WatchStatus.WATCHING,
        duration: Long = 0,
        createdAt: Date = Date(),
        updatedAt: Date = createdAt,
    ) = EpisodeProgress(
        id = id,
        mediaId = mediaId,
        ownerId = ownerId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        progress = progress,
        status = status,
        duration = duration,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun getUser(
        id: String = TEST_USER_ID,
        name: String = "Test User",
        image: Int = 1,
        pin: String? = null,
        pinHint: String? = null,
    ) = User(
        id = id,
        name = name,
        image = image,
        pin = pin,
        pinHint = pinHint,
    )

    fun getSearchHistory(
        id: Int = 0,
        query: String = "Test Query",
        ownerId: String = TEST_USER_ID,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
    ) = SearchHistory(
        id = id,
        query = query,
        ownerId = ownerId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun getLibraryList(
        id: String = "test-list-id",
        name: String = "Test List",
        description: String = "Test Description",
        ownerId: String = TEST_USER_ID,
        listType: LibraryListType = LibraryListType.CUSTOM,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
    ) = LibraryList(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        listType = listType,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun getLibraryListItem(
        id: Long = 0,
        mediaId: String = getDBMedia().id,
        listId: String = "test-list-id",
    ) = LibraryListItem(
        id = id,
        mediaId = mediaId,
        listId = listId,
    )

    fun getInstalledRepository(
        url: String = "https://example.com/repo",
        owner: String = "testowner",
        name: String = "testrepo",
        userId: String = TEST_USER_ID,
        rawLinkFormat: String = "https://raw.example.com/%s",
    ) = InstalledRepository(
        url = url,
        owner = owner,
        name = name,
        rawLinkFormat = rawLinkFormat,
        userId = userId,
    )

    fun getInstalledProvider(
        id: String = "test-provider",
        repositoryUrl: String = "https://example.com/repo",
        filePath: String = "provider.json",
        ownerId: String = TEST_USER_ID,
        sortOrder: Double = 1.0,
        isEnabled: Boolean = true,
        isDebug: Boolean = false,
    ) = InstalledProvider(
        id = id,
        repositoryUrl = repositoryUrl,
        sortOrder = sortOrder,
        ownerId = ownerId,
        filePath = filePath,
        isEnabled = isEnabled,
        isDebug = isDebug,
    )

    fun createDatabase(context: Context) =
        Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries()
            .build()
}
