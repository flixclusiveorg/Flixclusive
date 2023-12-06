package com.flixclusive.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flixclusive.common.Constants.APP_DATABASE
import com.flixclusive.data.database.converter.FilmDataConverter
import com.flixclusive.data.database.converter.WatchHistoryItemConverter
import com.flixclusive.data.database.dao.UserDao
import com.flixclusive.data.database.dao.WatchHistoryDao
import com.flixclusive.data.database.dao.WatchlistDao
import com.flixclusive.di.IoDispatcher
import com.flixclusive.domain.model.entities.User
import com.flixclusive.domain.model.entities.WatchHistoryItem
import com.flixclusive.domain.model.entities.WatchlistItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

@Database(
    entities = [WatchHistoryItem::class, WatchlistItem::class, User::class],
    version = 2
)
@TypeConverters(FilmDataConverter::class, WatchHistoryItemConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val MIGRATION_1_2: Migration = object: Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add watchlist table
                db.execSQL("CREATE TABLE IF NOT EXISTS `watchlist` (`ownerId` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY NOT NULL, `film` TEXT NOT NULL)")
                // Add user table
                db.execSQL("CREATE TABLE IF NOT EXISTS `User` (`userId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `image` INTEGER NOT NULL)")
                // Edit watch history table to add ownerId
                db.execSQL("ALTER TABLE `watch_history` ADD COLUMN ownerId INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(
            context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher
        ): AppDatabase {
            val scope = CoroutineScope(ioDispatcher)

            // For migration (1 to 2)
            context.updateOldDatabase()
            // ======================

            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    APP_DATABASE
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(
                        object: Callback() {
                            private fun prepopulateUsers() {
                                INSTANCE?.let {
                                    scope.launch {
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