package com.flixclusive.core.database.util

import androidx.room.TypeConverter
import com.flixclusive.core.util.network.fromJson
import com.flixclusive.model.tmdb.DBFilm
import com.google.gson.Gson

internal class FilmDataConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromFilmData(filmData: DBFilm): String {
        return gson.toJson(filmData)
    }

    @TypeConverter
    fun toFilmData(filmDataString: String): DBFilm {
        return fromJson<DBFilm>(filmDataString)
    }
}