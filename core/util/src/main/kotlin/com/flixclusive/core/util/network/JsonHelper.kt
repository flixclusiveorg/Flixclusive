package com.flixclusive.core.util.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Parses the specified JSON string into an object of type [T] using Gson library.
 *
 * @param json The JSON string to parse.
 * @return The parsed object of type [T].
 * @throws JsonSyntaxException if the JSON is not well-formed or cannot be parsed.
 */
inline fun <reified T> fromJson(json: String): T {
    return Gson().fromJson(json, object : TypeToken<T>() {}.type)
}
