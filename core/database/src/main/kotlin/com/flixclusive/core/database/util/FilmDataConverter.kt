package com.flixclusive.core.database.util

import androidx.room.TypeConverter
import com.flixclusive.model.tmdb.FilmImpl
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal class FilmDataConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromFilmData(filmData: FilmImpl): String {
        return gson.toJson(filmData)
    }

    @TypeConverter
    fun toFilmData(filmDataString: String): FilmImpl {
        val listType = object : TypeToken<FilmImpl>() {}.type
        return gson.fromJson(filmDataString, listType)
    }
}