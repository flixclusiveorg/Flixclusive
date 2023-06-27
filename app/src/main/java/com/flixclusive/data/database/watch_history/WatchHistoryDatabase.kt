package com.flixclusive.data.database.watch_history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flixclusive.data.database.common.FilmDataConverter
import com.flixclusive.data.database.watch_history.converter.WatchHistoryItemConverter
import com.flixclusive.domain.model.entities.WatchHistoryItem

@Database(entities = [WatchHistoryItem::class], version = 1, exportSchema = false)
@TypeConverters(WatchHistoryItemConverter::class, FilmDataConverter::class)
abstract class WatchHistoryDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: WatchHistoryDatabase? = null

        fun getInstance(context: Context): WatchHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    WatchHistoryDatabase::class.java,
                    "watch_history_database"
                )
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}