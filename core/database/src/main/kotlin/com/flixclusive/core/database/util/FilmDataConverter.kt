package com.flixclusive.core.database.util

import androidx.room.TypeConverter
import com.flixclusive.core.util.network.json.fromJson
import com.flixclusive.model.film.DBFilm
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser

internal class FilmDataConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromFilmData(filmData: DBFilm): String {
        return gson.toJson(filmData)
    }

    @TypeConverter
    fun toFilmData(filmDataString: String): DBFilm {
        val json = JsonParser.parseString(filmDataString)
        try { json.migrateToSchema4() }
        catch (_: Exception) {}

        return fromJson<DBFilm>(json)
    }

    private fun JsonElement.migrateToSchema4() {
        val json = asJsonObject

        val releaseDate = json.get("dateReleased").asString
        json.addProperty("releaseDate", releaseDate)

        val tmdbId = json.get("id").asInt
        json.addProperty("tmdbId", tmdbId)
        json.addProperty("id", "")

        val runtime = json.get("runtime").asJsonPrimitive
        if (!runtime.isJsonNull && runtime.isString) {
            json.remove("runtime")
        }

        json.add("recommendations", JsonArray())
    }
}