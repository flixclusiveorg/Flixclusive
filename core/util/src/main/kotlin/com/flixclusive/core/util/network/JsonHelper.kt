package com.flixclusive.core.util.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.io.Reader

/**
 * Parses the specified JSON string into an object of type [T] using Gson library.
 *
 * @param json The JSON string to parse.
 * @return The parsed object of type [T].
 * @throws JsonSyntaxException if the JSON is not well-formed or cannot be parsed.
 */
inline fun <reified T> fromJson(json: String): T {
    return Gson()
        .fromJson(json, object : TypeToken<T>() {}.type)
}

inline fun <reified T> fromJson(
    reader: Reader
): T = Gson()
    .fromJson(reader, object : TypeToken<T>() {}.type)

/**
 * Parses the specified [JsonElement] into an object of type [T] using Gson library.
 *
 * @param json The [JsonElement] to parse.
 * @return The parsed object of type [T].
 * @throws JsonSyntaxException if the JSON is not well-formed or cannot be parsed.
 */
inline fun <reified T> fromJson(json: JsonElement): T {
    return Gson()
        .fromJson(json, object : TypeToken<T>() {}.type)
}

/**
 * Parses the specified JSON string into an object of type [T] using Gson library.
 * With additional parameter to add your custom deserializer.
 *
 * @param json The JSON string to parse.
 * @param serializer The JSON deserializer to use for parsing.
 * @return The parsed object of type [T].
 * @throws JsonSyntaxException if the JSON is not well-formed or cannot be parsed.
 */
inline fun <reified T> fromJson(
    json: String,
    serializer: JsonDeserializer<T>
): T {
    return GsonBuilder()
        .registerTypeAdapter(T::class.java, serializer)
        .create()
        .fromJson(json, object : TypeToken<T>() {}.type)
}
