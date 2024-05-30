package com.flixclusive.core.util.network

import com.flixclusive.core.util.exception.safeCall
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject


const val USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.3) Gecko/20090912 Gentoo Firefox/3.5.3 FirePHP/0.3"
/**
 * Enum class representing HTTP request methods.
 * @property requiresBody Indicates whether the HTTP method typically requires a request body or data.
 * @see HttpMethod.GET
 * @see HttpMethod.POST
 * @see HttpMethod.PUT
 * @see HttpMethod.DELETE
 * @see HttpMethod.PATCH
 * @see HttpMethod.HEAD
 * @see HttpMethod.OPTIONS
 * @see HttpMethod.TRACE
 */
enum class HttpMethod(val requiresBody: Boolean = false) {
    GET,
    POST(requiresBody = true),
    PUT(requiresBody = true),
    DELETE,
    PATCH(requiresBody = true),
    HEAD,
    OPTIONS,
    TRACE
}

/**
 * Creates a form request using HTTP POST method.
 * @param url The URL for the request.
 * @param method The HTTP method for the request.
 * @param body The form data to be sent with the request.
 * @param headers The headers for the request.
 * @param userAgent The User-Agent header for the request.
 * @return A [Call] object representing the request.
 */
fun OkHttpClient.formRequest(
    url: String,
    method: HttpMethod,
    body: Map<String, String>,
    headers: Headers = Headers.headersOf(),
    userAgent: String = USER_AGENT,
): Call {
    require(method.requiresBody) {
        "The method request doesn't require a body. Are you using a function that requires a body?"
    }

    return request(
        url = url,
        method = method,
        body = body.toFormBody(),
        headers = headers,
        userAgent = userAgent
    )
}

/**
 * Creates a JSON request using the specified HTTP method.
 * @param url The URL for the request.
 * @param method The HTTP method for the request.
 * @param json The JSON data to be sent with the request. It could be a json string.
 * @param headers The headers for the request.
 * @param userAgent The User-Agent header for the request.
 * @return A [Call] object representing the request.
 */
fun OkHttpClient.jsonRequest(
    url: String,
    method: HttpMethod,
    json: Any,
    headers: Headers = Headers.headersOf(),
    userAgent: String = USER_AGENT,
): Call {
    require(method.requiresBody) {
        "The method request doesn't require a body. Are you using a function that requires a body?"
    }

    return request(
        url = url,
        method = method,
        body = jsonToRequestBody(json),
        headers = headers,
        userAgent = userAgent
    )
}

/**
 * Creates a request with a generic body using the specified HTTP method.
 * @param url The URL for the request.
 * @param method The HTTP method for the request.
 * @param body The body data to be sent with the request.
 * @param mediaType The [MediaType] of the request body.
 * @param headers The headers for the request.
 * @param userAgent The User-Agent header for the request.
 * @return A [Call] object representing the request.
 */
fun OkHttpClient.genericBodyRequest(
    url: String,
    method: HttpMethod,
    body: String,
    mediaType: MediaType?,
    headers: Headers = Headers.headersOf(),
    userAgent: String = USER_AGENT,
): Call {
    require(method.requiresBody) {
        "The method request doesn't require a body. Are you using a function that requires a body?"
    }

    return request(
        url = url,
        method = method,
        body = body.toRequestBody(mediaType),
        headers = headers,
        userAgent = userAgent
    )
}

/**
 * Creates a generic HTTP request.
 * @param url The URL for the request.
 * @param method The HTTP method for the request.
 * @param body The body data to be sent with the request.
 * @param headers The headers for the request.
 * @param userAgent The User-Agent header for the request.
 * @return A [Call] object representing the request.
 */
fun OkHttpClient.request(
    url: String,
    method: HttpMethod = HttpMethod.GET,
    body: RequestBody? = null,
    headers: Headers = Headers.headersOf(),
    userAgent: String = USER_AGENT,
): Call {
    val headersToUse = Headers.Builder()
        .add("User-Agent", userAgent)
        .addAll(headers)
        .build()

    var request = Request.Builder()
        .url(url)
        .headers(headersToUse)

    if (method.requiresBody) {
        request = request
            .method(method.name, body)
    } else if (body != null) {
        throw IllegalArgumentException("The request has a body but the method doesn't require one. Consider using the correct method request.")
    }

    return newCall(request.build())
}

/**
 * Converts a map of key-value pairs to a [FormBody] object.
 * @receiver The map to be converted.
 * @return The converted [FormBody].
 */
private fun Map<String, String>.toFormBody(): FormBody {
    val builder = FormBody.Builder()
    forEach {
        builder.addEncoded(it.key, it.value)
    }
    return builder.build()
}

/**
 * Converts JSON data to a [RequestBody] object.
 * @param data The JSON data to be converted.
 * @return The converted [RequestBody].
 * @throws IllegalArgumentException if the provided request data is not a valid JSON object.
 */
private fun jsonToRequestBody(data: Any): RequestBody {
    val jsonString = when {
        data is JSONObject -> data.toString()
        data is JSONArray -> data.toString()
        data is String && isJson(data) -> data
        else -> throw IllegalArgumentException("The provided request data is not a valid JSON object.")
    }

    val jsonType = "application/json;charset=utf-8".toMediaTypeOrNull()

    return jsonString.toRequestBody(jsonType)
}

/**
 * Checks if a string represents a valid JSON object.
 * @param test The string to be checked.
 * @return `true` if the string represents a valid JSON object, `false` otherwise.
 */
private fun isJson(test: String): Boolean {
    return safeCall {
        JSONObject(test)
        true
    } ?: safeCall {
        JSONArray(test)
        true
    } ?: false
}