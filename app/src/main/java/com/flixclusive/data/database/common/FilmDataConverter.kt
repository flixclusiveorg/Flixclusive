package com.flixclusive.data.database.common

import androidx.room.TypeConverter
import com.flixclusive.domain.model.tmdb.FilmImpl
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FilmDataConverter {
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