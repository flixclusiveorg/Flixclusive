package com.flixclusive.core.network.util.serializers

import com.flixclusive.core.network.retrofit.dto.TMDBSearchItem
import com.flixclusive.core.network.retrofit.dto.toFilmSearchItem
import com.flixclusive.model.film.util.FilmType
import com.flixclusive.model.film.util.FilmType.Companion.toFilmType
import com.flixclusive.model.film.FilmSearchItem
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

internal class FilmSearchItemDeserializer : JsonDeserializer<FilmSearchItem> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): FilmSearchItem? {
        val resultJsonObject =
            json?.asJsonObject ?: JsonObject()

        val mediaTypeObject = resultJsonObject.get("media_type")
        val firstAirDate = resultJsonObject.get("first_air_date")
        val releaseDate = resultJsonObject.get("release_date")

        val filmType = when {
            mediaTypeObject != null -> mediaTypeObject.asString.toFilmType()
            firstAirDate != null -> FilmType.TV_SHOW
            releaseDate != null -> FilmType.MOVIE
            else -> throw IllegalArgumentException("Unknown media type: $resultJsonObject")
        }

        val result = context?.deserialize<TMDBSearchItem>(
            resultJsonObject,
            TMDBSearchItem::class.java
        )

        return result?.toFilmSearchItem(filmType)
    }
}