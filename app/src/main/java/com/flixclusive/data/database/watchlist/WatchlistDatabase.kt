package com.flixclusive.data.database.watchlist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flixclusive.data.database.common.FilmDataConverter
import com.flixclusive.domain.model.entities.WatchlistItem

@Database(entities = [WatchlistItem::class], version = 1, exportSchema = false)
@TypeConverters(FilmDataConverter::class)
abstract class WatchlistDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile
        private var INSTANCE: WatchlistDatabase? = null

        fun getInstance(context: Context): WatchlistDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    WatchlistDatabase::class.java,
                    "watchlist_database"
                )
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}
