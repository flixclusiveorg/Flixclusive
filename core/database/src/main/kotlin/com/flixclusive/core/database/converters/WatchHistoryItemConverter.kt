package com.flixclusive.core.database.converters

import androidx.room.TypeConverter
import com.flixclusive.core.database.entity.EpisodeWatched
import com.flixclusive.core.util.network.json.fromJson
import com.google.gson.Gson

class WatchHistoryItemConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromEpisodeWatchedList(value: List<EpisodeWatched>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toEpisodeWatchedList(value: String): List<EpisodeWatched> {
        return fromJson<List<EpisodeWatched>>(value)
    }

    @TypeConverter
    fun fromEpisodeLimitsMap(map: Map<Int, Int>): String {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toEpisodeLimitsMap(json: String): Map<Int, Int> {
        return fromJson<Map<Int, Int>>(json)
    }
}
