package com.flixclusive.provider.base.util

import com.flixclusive.provider.base.util.RequestBodyType.Companion.toMediaTypeOrNull
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.Reader

const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36"

private val mustHaveBody = listOf("POST", "PUT")

enum class RequestBodyType(val type: String) {
    JSON("application/json;charset=utf-8"),
    TEXT("text/plain;charset=utf-8");

    companion object {
        fun RequestBodyType.toMediaTypeOrNull(): MediaType? {
            return type.toMediaTypeOrNull()
        }
    }
}

fun Reader?.asString(): String? {
    return use {
        val string = it?.readText()
        it?.close()

        return@use string
    }
}

fun GET(
    url: String,
    headers: Headers = Headers.headersOf(),
    userAgent: String = USER_AGENT,
): Request {
    val headersToUse = Headers.Builder()
        .add("User-Agent", userAgent)
        .addAll(headers)
        .build()

    return Request.Builder()
        .url(url)
        .headers(headersToUse)
        .build()
}

fun POST(
    url: String,
    data: Map<String, String>? = null,
    json: Any? = null,
    headers: Headers = Headers.headersOf(),
    userAgent: String = USER_AGENT,
): Request {
    val method = "POST"
    val headersToUse = Headers.Builder()
        .add("User-Agent", userAgent)
        .addAll(headers)
        .build()

    val body = getRequestBody(method, data, json)

    return Request.Builder()
        .url(url)
        .method(method, body)
        .headers(headersToUse)
        .build()
}

private fun Map<String, String>.toFormBody(): FormBody {
    val builder = FormBody.Builder()
    forEach {
        builder.addEncoded(it.key, it.value)
    }
    return builder.build()
}

private fun jsonToRequestBody(data: Any): RequestBody {
    val jsonString = when (data) {
        is JSONObject -> data.toString()
        is JSONArray -> data.toString()
        is String -> data
        else -> data.toString()
    }

    val type = if (data is String)
        RequestBodyType.TEXT
    else RequestBodyType.JSON

    return jsonString.toRequestBody(type.toMediaTypeOrNull())
}

private fun getRequestBody(
    method: String,
    data: Map<String, String>? = null,
    json: Any? = null,
): RequestBody? {
    return when {
        data != null -> data.toFormBody()
        json != null -> jsonToRequestBody(json)
        else -> if(mustHaveBody.contains(method)) FormBody.Builder().build() else null
    }
}