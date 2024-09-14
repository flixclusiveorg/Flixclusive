package com.flixclusive.core.network.util.serializers

import com.flixclusive.core.network.retrofit.dto.TMDBTvShow
import com.flixclusive.core.network.retrofit.dto.toTvShowDetails
import com.flixclusive.model.film.TvShow
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

internal class TMDBTvShowDeserializer : JsonDeserializer<TvShow> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): TvShow? {
        val resultJsonObject =
            json?.asJsonObject ?: JsonObject()

        val result = context?.deserialize<TMDBTvShow>(
            resultJsonObject,
            TMDBTvShow::class.java
        )

        return result?.toTvShowDetails()
    }
}