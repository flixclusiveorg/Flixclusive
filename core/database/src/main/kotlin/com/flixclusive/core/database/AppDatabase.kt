package com.flixclusive.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flixclusive.core.database.dao.SearchHistoryDao
import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.database.dao.WatchHistoryDao
import com.flixclusive.core.database.dao.WatchlistDao
import com.flixclusive.core.database.migration.Schema1to2
import com.flixclusive.core.database.migration.Schema2to3
import com.flixclusive.core.database.migration.Schema3to4
import com.flixclusive.core.database.migration.Schema4to5
import com.flixclusive.core.database.util.DateConverter
import com.flixclusive.core.database.util.FilmDataConverter
import com.flixclusive.core.database.util.WatchHistoryItemConverter
import com.flixclusive.core.util.coroutines.AppDispatchers
import com.flixclusive.model.database.SearchHistory
import com.flixclusive.model.database.User
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.WatchlistItem
import kotlinx.coroutines.launch
import java.io.File


private const val APP_DATABASE = "app_database"

@Database(
    entities = [WatchHistoryItem::class, WatchlistItem::class, User::class, SearchHistory::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(FilmDataConverter::class, WatchHistoryItemConverter::class, DateConverter::class)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(
            context: Context
        ): AppDatabase {
            // For migration (1 to 2)
            context.updateOldDatabase()
            // ======================

            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    APP_DATABASE
                )
                    .addMigrations(
                        Schema1to2(),
                        Schema2to3(),
                        Schema3to4(),
                        Schema4to5(),
                    )
                    .addCallback(
                        object: Callback() {
                            private fun prepopulateUsers() {
                                INSTANCE?.let {
                                    AppDispatchers.Default.scope.launch {
                                        val thereAreNoUsers = it.userDao().getAllItems().isEmpty()

                                        if(thereAreNoUsers) {
                                            it.userDao().insert(User())
                                        }
                                    }
                                }
                            }

                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                prepopulateUsers()
                            }

                            override fun onOpen(db: SupportSQLiteDatabase) {
                                super.onOpen(db)
                                prepopulateUsers()
                            }
                        }
                    )
                    .build()
                    .also {
                        INSTANCE = it
                    }
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

            if(oldDbFile.exists()) {
                oldDbFile.renameTo(
                    File(oldDbFile.path.replace(oldDatabaseName, APP_DATABASE))
                )
            }

            if(oldDbShmFile.exists()) {
                oldDbShmFile.renameTo(
                    File(oldDbShmFile.path.replace("$oldDatabaseName-shm", "$APP_DATABASE-shm"))
                )
            }

            if(oldDbWalFile.exists()) {
                oldDbWalFile.renameTo(
                    File(oldDbWalFile.path.replace("$oldDatabaseName-wal", "$APP_DATABASE-wal"))
                )
            }
        }
    }
}