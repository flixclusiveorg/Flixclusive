package com.flixclusive.core.testing.database

import android.content.Context
import androidx.room.Room
import com.flixclusive.core.database.AppDatabase
import com.flixclusive.core.database.entity.DBFilm
import com.flixclusive.core.database.entity.EpisodeWatched
import com.flixclusive.core.database.entity.LibraryList
import com.flixclusive.core.database.entity.LibraryListItem
import com.flixclusive.core.database.entity.SearchHistory
import com.flixclusive.core.database.entity.User
import com.flixclusive.core.database.entity.WatchHistory
import com.flixclusive.core.database.entity.WatchlistItem
import com.flixclusive.core.database.entity.toDBFilm
import com.flixclusive.core.testing.film.FilmTestDefaults
import java.util.Date

/**
 * Test defaults for database-related tests.
 * */
object DatabaseTestDefaults {
    const val TEST_USER_ID = 1

    fun getWatchlistItem(
        id: String = "watchlist_item_123",
        ownerId: Int = TEST_USER_ID,
        film: DBFilm = FilmTestDefaults.getMovie().toDBFilm(),
        addedOn: Date = Date(),
    ) = WatchlistItem(
        id = id,
        ownerId = ownerId,
        film = film,
        addedOn = addedOn,
    )

    fun getWatchHistoryItem(
        seasons: Int = 1,
        episodes: Map<Int, Int> = mapOf(
            1 to 1, // Season 1 with 5 episodes
            2 to 3, // Season 2 with 3 episodes
        ),
        episodesWatched: List<EpisodeWatched> = emptyList(),
        id: String = "watch_history_item_123",
        ownerId: Int = TEST_USER_ID,
        dateWatched: Date = Date(),
        film: DBFilm = FilmTestDefaults.getMovie().toDBFilm(),
    ) = WatchHistory(
        id = id,
        seasons = seasons,
        ownerId = ownerId,
        episodesWatched = episodesWatched,
        episodes = episodes,
        film = film,
        dateWatched = dateWatched,
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
        id: Int = 1,
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
        id: Int = 1,
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
        id: String = "library_list_item_123",
        film: DBFilm = FilmTestDefaults.getMovie().toDBFilm(),
    ) = LibraryListItem(
        id = id,
        film = film,
    )

    fun createDatabase(context: Context) =
        Room
            .inMemoryDatabaseBuilder(
                context = context,
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries()
            .build()
}
