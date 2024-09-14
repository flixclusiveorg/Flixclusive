package com.flixclusive.core.network.util.serializers

import com.flixclusive.core.network.retrofit.dto.TMDBMovie
import com.flixclusive.core.network.retrofit.dto.toMovieDetails
import com.flixclusive.model.film.Movie
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

internal class TMDBMovieDeserializer : JsonDeserializer<Movie> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): Movie? {
        val resultJsonObject =
            json?.asJsonObject ?: JsonObject()

        val result = context?.deserialize<TMDBMovie>(
            resultJsonObject,
            TMDBMovie::class.java
        )

        return result?.toMovieDetails()
    }
}