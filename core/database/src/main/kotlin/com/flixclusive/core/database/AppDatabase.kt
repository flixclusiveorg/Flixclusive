package com.flixclusive.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flixclusive.core.database.dao.UserDao
import com.flixclusive.core.database.dao.WatchHistoryDao
import com.flixclusive.core.database.dao.WatchlistDao
import com.flixclusive.core.database.util.DateConverter
import com.flixclusive.core.database.util.FilmDataConverter
import com.flixclusive.core.database.util.WatchHistoryItemConverter
import com.flixclusive.core.util.common.dispatcher.di.ApplicationScope
import com.flixclusive.model.database.User
import com.flixclusive.model.database.WatchHistoryItem
import com.flixclusive.model.database.WatchlistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File


const val APP_DATABASE = "app_database"

@Database(
    entities = [WatchHistoryItem::class, WatchlistItem::class, User::class],
    version = 4
)
@TypeConverters(FilmDataConverter::class, WatchHistoryItemConverter::class, DateConverter::class)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(
            context: Context,
            @ApplicationScope scope: CoroutineScope
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
                        DatabaseMigrations.Schema1to2(),
                        DatabaseMigrations.Schema2to3(),
                        DatabaseMigrations.Schema3to4(),
                    )
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

        /**
         *
         * This would be deleted on soon updates.
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