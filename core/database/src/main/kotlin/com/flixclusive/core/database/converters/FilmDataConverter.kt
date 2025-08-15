package com.flixclusive.core.database.converters

import androidx.room.TypeConverter
import com.flixclusive.core.database.entity.DBFilm
import com.flixclusive.core.util.network.json.fromJson
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser

class FilmDataConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromFilmData(filmData: DBFilm): String {
        return gson.toJson(filmData)
    }

    @TypeConverter
    fun toFilmData(filmDataString: String): DBFilm {
        val json = JsonParser.parseString(filmDataString)

        runCatching {
            json.migrateToSchema4()
        }

        runCatching {
            json.migrateToSchema8()
        }

        return fromJson<DBFilm>(json)
    }

    private fun JsonElement.migrateToSchema8() {
        val json = asJsonObject

        if (json.has("providerName")) {
            json.addProperty("providerId", json.get("providerName").asString)
            json.remove("providerName")
        }

        if (json.has("recommendations")) {
            val recommendations = json.get("recommendations").asJsonArray

            recommendations.forEach { recommendation ->
                with(recommendation.asJsonObject) {
                    if (has("providerName")) {
                        addProperty("providerId", get("providerName").asString)
                        remove("providerName")
                    }
                }
            }
        }
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
