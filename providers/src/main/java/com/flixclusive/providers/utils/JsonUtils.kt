package com.flixclusive.providers.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

internal object JsonUtils {

    inline fun <reified T> fromJson(json: String): T {
        return Gson().fromJson(json, object : TypeToken<T>() {}.type)
    }
}