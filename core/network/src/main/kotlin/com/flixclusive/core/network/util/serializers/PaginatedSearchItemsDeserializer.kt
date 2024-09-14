package com.flixclusive.core.network.util.serializers

import com.flixclusive.model.film.FilmSearchItem
import com.flixclusive.model.film.SearchResponseData
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type


internal class PaginatedSearchItemsDeserializer : JsonDeserializer<SearchResponseData<FilmSearchItem>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): SearchResponseData<FilmSearchItem> {
        val jsonObject = json?.asJsonObject
            ?: JsonObject()

        val page = jsonObject.get("page").asInt
        val totalPages = jsonObject.get("total_pages").asInt

        // Get the results JsonArray from the JsonObject
        val resultsJsonArray = jsonObject.getAsJsonArray("results")

        // Create an empty list to hold the results
        val results = mutableListOf<FilmSearchItem>()

        // Loop through the results JsonArray and deserialize each result into a FilmSearchItem
        // (in this case, a TvShowTMDBSearchItem) and add it to the results list
        resultsJsonArray.forEach {
            if (it.isPerson()) {
                return@forEach
            }

            context?.deserialize<FilmSearchItem>(
                /* json = */ it,
                /* typeOfT = */ FilmSearchItem::class.java,
            )?.let(results::add)
        }

        // Create and return a new TMDBPageResponse object with the currentPage, results, total pages, and total results counts
        return SearchResponseData(
            page = page,
            results = results,
            totalPages = totalPages
        )
    }

    private fun JsonElement.isPerson(): Boolean {
        return try {
            asJsonObject.get("media_type").asString.equals("person", true)
        } catch (_: Exception) {
            false
        }
    }
}
