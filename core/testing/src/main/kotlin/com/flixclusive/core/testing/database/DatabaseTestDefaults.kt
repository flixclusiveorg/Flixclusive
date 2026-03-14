package com.flixclusive.core.testing.database

import android.content.Context
import androidx.room.Room
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListType
import com.flixclusive.core.database.entity.provider.InstalledProvider
import com.flixclusive.core.database.entity.provider.InstalledRepository
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.model.film.Film

/**
 * Test defaults for database-related tests.
 * */
object DatabaseTestDefaults {
    const val TEST_USER_ID = 1

    fun getDBFilm(film: Film = FilmTestDefaults.getMovie()) = film.toDBFilm()

    fun getMovieProgress(
        id: Long = 0,
        itemId: Long = 1,
        progress: Long = 0,
        duration: Long = 0,
        watchStatus: String = WatchStatus.WATCHING.name,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
    ) = MovieProgress(
        id = id,
        itemId = itemId,
        progress = progress,
        duration = duration,
        status = watchStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun getEpisodeProgress(
        id: Long = 0,
        itemId: Long = 1,
        seasonNumber: Int = 1,
        episodeNumber: Int = 1,
        progress: Long = 0,
        duration: Long = 0,
        watchStatus: String = WatchStatus.WATCHING.name,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
    ) = EpisodeProgress(
        id = id,
        itemId = itemId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        progress = progress,
        duration = duration,
        status = watchStatus,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun getUser(
        id: Int = TEST_USER_ID,
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
        ownerId: Int = TEST_USER_ID,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
    ) = SearchHistory(
        id = id,
        query = query,
        ownerId = ownerId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun getLibraryList(
        id: Int = 0,
        name: String = "Test List",
        description: String = "Test Description",
        ownerId: Int = TEST_USER_ID,
        listType: String = LibraryListType.CUSTOM.name,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
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
        filmId: String = getDBFilm().id,
        listId: Int = 1,
    ) = LibraryListItem(
        id = id,
        filmId = filmId,
        listId = listId,
    )

    fun getRepositoryEntity(
        url: String = "https://example.com/repo",
        owner: String = "testowner",
        name: String = "testrepo",
        rawLinkFormat: String = "https://raw.example.com/%s",
    ) = InstalledRepository(
        url = url,
        owner = owner,
        name = name,
        rawLinkFormat = rawLinkFormat,
    )

    fun getInstalledProviderEntity(
        id: String = "test-provider",
        repositoryUrl: String = "https://example.com/repo",
        name: String = "Test Provider",
        status: String = "active",
        providerType: String = "movie",
        language: String = "en",
        adult: Boolean = false,
        versionName: String = "1.0.0",
        versionCode: Int = 1,
        buildUrl: String = "https://example.com/build.flx",
        sortOrder: Double = 1.0,
    ) = InstalledProvider(
        id = id,
        repositoryUrl = repositoryUrl,
        name = name,
        status = status,
        providerType = providerType,
        language = language,
        adult = adult,
        versionName = versionName,
        versionCode = versionCode,
        buildUrl = buildUrl,
        sortOrder = sortOrder,
    )

    fun createDatabase(context: Context) =
        Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries()
            .build()
}
