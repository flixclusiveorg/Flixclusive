package com.flixclusive.core.network.util

import com.flixclusive.model.tmdb.TMDBPageResponse
import com.flixclusive.model.tmdb.TMDBSearchItem
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type


class PaginatedSearchItemsDeserializer : JsonDeserializer<TMDBPageResponse<TMDBSearchItem>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): TMDBPageResponse<TMDBSearchItem> {
        // Get the JsonObject from the JsonElement or create a new one if it's null
        val jsonObject = json?.asJsonObject ?: JsonObject()

        // Get the page, total pages, and total results counts from the JsonObject
        val page = jsonObject.get("page").asInt
        val totalPages = jsonObject.get("total_pages").asInt
        val totalResults = jsonObject.get("total_results").asInt

        // Get the results JsonArray from the JsonObject
        val resultsJsonArray = jsonObject.getAsJsonArray("results")

        // Create an empty list to hold the results
        val results = mutableListOf<TMDBSearchItem>()

        // Loop through the results JsonArray and deserialize each result into a TMDBSearchItem
        // (in this case, a TvShowTMDBSearchItem) and add it to the results list
        for (resultJson in resultsJsonArray) {
            val result: TMDBSearchItem =
                context?.deserialize(resultJson, TMDBSearchItem::class.java)
                    ?: TMDBSearchItem.MovieTMDBSearchItem()
            results.add(result)
        }

        // Create and return a new TMDBPageResponse object with the currentPage, results, total pages, and total results counts
        return TMDBPageResponse(
            page = page,
            results = results,
            totalPages = totalPages,
            totalResults = totalResults,
        )

    }
}
