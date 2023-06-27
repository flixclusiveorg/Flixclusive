package com.flixclusive.domain.common

import com.flixclusive.domain.model.tmdb.TMDBSearchItem
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

class SearchItemDeserializer : JsonDeserializer<TMDBSearchItem> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): TMDBSearchItem {
        val resultJsonObject =
            json?.asJsonObject ?: JsonObject() // ensure we have a JsonObject to work with

        val mediaTypeObject = resultJsonObject.get("media_type") // get the "media_type" property
        val firstAirDate =
            resultJsonObject.get("first_air_date") // get the "first_air_date" property
        val releaseDate = resultJsonObject.get("release_date") // get the "release_date" property

        // determine the media type based on the "media_type", "first_air_date", and "release_date" properties
        val mediaType = when {
            mediaTypeObject != null -> mediaTypeObject.asString
            firstAirDate != null -> "tv"
            releaseDate != null -> "movie"
            else -> throw IllegalArgumentException("Unknown media type: $resultJsonObject")
        }

        // deserialize the JsonObject based on its media type
        val result = when (mediaType) {
            "movie" -> context?.deserialize(
                resultJsonObject,
                TMDBSearchItem.MovieTMDBSearchItem::class.java
            ) ?: TMDBSearchItem.MovieTMDBSearchItem()

            "tv" -> context?.deserialize(
                resultJsonObject,
                TMDBSearchItem.TvShowTMDBSearchItem::class.java
            ) ?: TMDBSearchItem.TvShowTMDBSearchItem()

            "person" -> {
                // get the "known_for" property and recursively deserialize each into a TMDBSearchItem object
                val knownForArray = resultJsonObject.remove("known_for").asJsonArray

                val knownForList = mutableListOf<TMDBSearchItem>()
                for (knownFor in knownForArray) {
                    val knownForObject: TMDBSearchItem =
                        context?.deserialize(knownFor, TMDBSearchItem::class.java)
                            ?: TMDBSearchItem.MovieTMDBSearchItem()

                    knownForList.add(knownForObject)
                }

                // deserialize the rest of the JsonObject into a PersonTMDBSearchItem object
                context?.deserialize(resultJsonObject, TMDBSearchItem.PersonTMDBSearchItem::class.java)
                    ?: TMDBSearchItem.PersonTMDBSearchItem().copy(
                        knownFor = knownForList
                    )
            }

            else -> throw IllegalArgumentException("Unknown media type: $mediaType")
        }

        return result // return the deserialized object

    }
}