package com.flixclusive.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flixclusive.core.database.converters.DateConverter
import com.flixclusive.core.database.converters.MapConverter
import com.flixclusive.core.database.dao.DBFilmDao
import com.flixclusive.core.database.dao.EpisodeProgressDao
import com.flixclusive.core.database.dao.LibraryListDao
import com.flixclusive.core.database.dao.LibraryListItemDao
import com.flixclusive.core.database.dao.MovieProgressDao
import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.database.dao.WatchlistDao
import com.flixclusive.core.database.entity.film.DBFilm
import com.flixclusive.core.database.entity.library.LibraryList
import com.flixclusive.core.database.entity.library.LibraryListItem
import com.flixclusive.core.database.entity.library.LibraryListItemWithMetadata
import com.flixclusive.core.database.entity.search.SearchHistory
import com.flixclusive.core.database.entity.user.User
import com.flixclusive.core.database.entity.watched.EpisodeProgress
import com.flixclusive.core.database.entity.watched.MovieProgress
import com.flixclusive.core.database.entity.watchlist.Watchlist
import com.flixclusive.core.database.migration.Schema1to2
import com.flixclusive.core.database.migration.Schema2to3
import com.flixclusive.core.database.migration.Schema3to4
import com.flixclusive.core.database.migration.Schema4to5
import com.flixclusive.core.database.migration.Schema5to6
import com.flixclusive.core.database.migration.Schema6to7
import com.flixclusive.core.database.migration.Schema7to8
import com.flixclusive.core.database.migration.Schema8to9
import java.io.File

internal const val APP_DATABASE = "app_database"

@Database(
    entities = [
        DBFilm::class,
        LibraryList::class,
        LibraryListItem::class,
        SearchHistory::class,
        User::class,
        MovieProgress::class,
        EpisodeProgress::class,
        Watchlist::class,
    ],
    views = [LibraryListItemWithMetadata::class],
    version = 9,
    exportSchema = true,
)
@TypeConverters(
    DateConverter::class,
    MapConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun watchlistDao(): WatchlistDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    abstract fun libraryListDao(): LibraryListDao

    abstract fun libraryListItemDao(): LibraryListItemDao

    abstract fun filmsDao(): DBFilmDao

    abstract fun episodeProgressDao(): EpisodeProgressDao

    abstract fun movieProgressDao(): MovieProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // For migration (1 to 2)
            context.updateOldDatabase()
            // ======================

            return INSTANCE ?: synchronized(this) {
                Room
                    .databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        APP_DATABASE,
                    ).addMigrations(
                        Schema1to2,
                        Schema2to3,
                        Schema3to4,
                        Schema4to5,
                        Schema5to6,
                        Schema6to7,
                        Schema7to8,
                        Schema8to9,
                    ).build()
                    .also { INSTANCE = it }
            }
        }

        /**
         *
         * For ancient Flixclusive versions lol
         *
         * */
        private fun Context.updateOldDatabase() {
            val oldDatabaseName = "watch_history_database"

            val oldDbFile = applicationContext.getDatabasePath(oldDatabaseName)
            val oldDbShmFile = applicationContext.getDatabasePath("$oldDatabaseName-shm")
            val oldDbWalFile = applicationContext.getDatabasePath("$oldDatabaseName-wal")

            if (oldDbFile.exists()) {
                oldDbFile.renameTo(
                    File(oldDbFile.path.replace(oldDatabaseName, APP_DATABASE)),
                )
            }

            if (oldDbShmFile.exists()) {
                oldDbShmFile.renameTo(
                    File(oldDbShmFile.path.replace("$oldDatabaseName-shm", "$APP_DATABASE-shm")),
                )
            }

            if (oldDbWalFile.exists()) {
                oldDbWalFile.renameTo(
                    File(oldDbWalFile.path.replace("$oldDatabaseName-wal", "$APP_DATABASE-wal")),
                )
            }
        }
    }
}
