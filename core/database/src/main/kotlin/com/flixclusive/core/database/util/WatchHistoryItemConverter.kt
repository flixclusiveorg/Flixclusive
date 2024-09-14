package com.flixclusive.core.database.util

import androidx.room.TypeConverter
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.model.database.EpisodeWatched
import com.google.gson.Gson

internal class WatchHistoryItemConverter {
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
