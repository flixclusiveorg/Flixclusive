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
import java.util.Date

/**
 * Test defaults for database-related tests.
 * */
object DatabaseTestDefaults {
    const val TEST_USER_ID = 1

    fun getDBFilm(film: Film = FilmTestDefaults.getMovie()) = film.toDBFilm()

    fun getMovieProgress(
        id: Long = 0,
        filmId: String = getDBFilm().id,
        ownerId: Int = TEST_USER_ID,
        progress: Long = 0,
        status: WatchStatus = WatchStatus.WATCHING,
        duration: Long = 0,
        createdAt: Date = Date(),
        updatedAt: Date = createdAt,
    ) = MovieProgress(
        id = id,
        filmId = filmId,
        ownerId = ownerId,
        progress = progress,
        status = status,
        duration = duration,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun getEpisodeProgress(
        id: Long = 0,
        filmId: String = getDBFilm(film = FilmTestDefaults.getTvShow()).id,
        ownerId: Int = TEST_USER_ID,
        seasonNumber: Int = 1,
        episodeNumber: Int = 1,
        progress: Long = 0,
        status: WatchStatus = WatchStatus.WATCHING,
        duration: Long = 0,
        createdAt: Date = Date(),
        updatedAt: Date = createdAt,
    ) = EpisodeProgress(
        id = id,
        filmId = filmId,
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
        id: Int = 0,
        name: String = "Test List",
        description: String = "Test Description",
        ownerId: Int = TEST_USER_ID,
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
        userId: Int = TEST_USER_ID,
        rawLinkFormat: String = "https://raw.example.com/%s",
    ) = InstalledRepository(
        url = url,
        owner = owner,
        name = name,
        rawLinkFormat = rawLinkFormat,
        userId = userId,
    )

    fun getInstalledProviderEntity(
        id: String = "test-provider",
        repositoryUrl: String = "https://example.com/repo",
        filePath: String = "provider.json",
        ownerId: Int = TEST_USER_ID,
        sortOrder: Double = 1.0,
    ) = InstalledProvider(
        id = id,
        repositoryUrl = repositoryUrl,
        sortOrder = sortOrder,
        ownerId = ownerId,
        filePath = filePath,
    )

    fun createDatabase(context: Context) =
        Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries()
            .build()
}
