package com.flixclusive.core.database.util

import androidx.room.TypeConverter
import com.flixclusive.core.util.json.fromJson
import com.flixclusive.model.tmdb.FilmImpl
import com.google.gson.Gson

internal class FilmDataConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromFilmData(filmData: FilmImpl): String {
        return gson.toJson(filmData)
    }

    @TypeConverter
    fun toFilmData(filmDataString: String): FilmImpl {
        return fromJson<FilmImpl>(filmDataString)
    }
}