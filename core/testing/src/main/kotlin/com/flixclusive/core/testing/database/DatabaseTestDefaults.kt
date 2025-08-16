package com.flixclusive.core.testing.database

import android.content.Context
import androidx.room.Room
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.film.DBFilm.Companion.toDBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watched.WatchStatus
import com.flixclusive.core.database.entity.watchlist.Watchlist
import com.flixclusive.core.testing.film.FilmTestDefaults
import com.flixclusive.model.film.Film
import java.util.Date

/**
 * Test defaults for database-related tests.
 * */
object DatabaseTestDefaults {
    const val TEST_USER_ID = 1

    fun getDBFilm(film: Film = FilmTestDefaults.getMovie()) = film.toDBFilm()

    fun getWatchlistItem(
        id: Long = 0,
        ownerId: Int = TEST_USER_ID,
        filmId: String = getDBFilm().identifier,
        addedAt: Date = Date(),
    ) = Watchlist(
        id = id,
        ownerId = ownerId,
        filmId = filmId,
        addedAt = addedAt,
    )

    fun getMovieProgress(
        id: Long = 0,
        filmId: String = getDBFilm().identifier,
        ownerId: Int = TEST_USER_ID,
        progress: Long = 0,
        status: WatchStatus = WatchStatus.WATCHING,
        duration: Long = 0,
        watchedAt: Date = Date(),
        watchCount: Int = 1,
    ) = MovieProgress(
        id = id,
        filmId = filmId,
        ownerId = ownerId,
        progress = progress,
        status = status,
        duration = duration,
        watchedAt = watchedAt,
        watchCount = watchCount,
    )

    fun getEpisodeProgress(
        id: Long = 0,
        filmId: String = getDBFilm(film = FilmTestDefaults.getTvShow()).identifier,
        ownerId: Int = TEST_USER_ID,
        seasonNumber: Int = 1,
        episodeNumber: Int = 1,
        progress: Long = 0,
        status: WatchStatus = WatchStatus.WATCHING,
        duration: Long = 0,
        watchedAt: Date = Date(),
    ) = EpisodeProgress(
        id = id,
        filmId = filmId,
        ownerId = ownerId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        progress = progress,
        status = status,
        duration = duration,
        watchedAt = watchedAt,
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
        searchedOn: Date = Date(),
    ) = SearchHistory(
        id = id,
        query = query,
        ownerId = ownerId,
        searchedOn = searchedOn,
    )

    fun getLibraryList(
        id: Int = 0,
        name: String = "Test List",
        description: String = "Test Description",
        ownerId: Int = TEST_USER_ID,
        createdAt: Date = Date(),
        updatedAt: Date = Date(),
    ) = LibraryList(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun getLibraryListItem(
        id: Long = 0,
        filmId: String = getDBFilm().identifier,
        listId: Int = 1,
    ) = LibraryListItem(
        id = id,
        filmId = filmId,
        listId = listId,
    )

    fun createDatabase(context: Context) =
        Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries()
            .build()
}
