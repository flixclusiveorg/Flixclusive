package com.flixclusive.core.database.util

import androidx.room.TypeConverter
import com.flixclusive.model.database.EpisodeWatched
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

internal class WatchHistoryItemConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromEpisodeWatchedList(value: List<EpisodeWatched>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toEpisodeWatchedList(value: String): List<EpisodeWatched> {
        val type = object : TypeToken<List<EpisodeWatched>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromEpisodeLimitsMap(map: Map<Int, Int>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toEpisodeLimitsMap(json: String): Map<Int, Int> {
        val type = object : TypeToken<Map<Int, Int>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromDate(value: Date): Long {
        return value.time
    }

    @TypeConverter
    fun toDate(value: Long): Date {
        return Date(value)
    }
}
