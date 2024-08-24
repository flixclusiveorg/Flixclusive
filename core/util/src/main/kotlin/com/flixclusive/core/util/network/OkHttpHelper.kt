package com.flixclusive.core.util.network

import com.flixclusive.core.util.exception.safeCall
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import kotlin.random.Random


const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; SM-A145M Build/TP1A.220624.014; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/127.0.6533.103 Mobile Safari/537.36 [FB_IAB/FB4A;FBAV/478.0.0.41.86;]"

fun getRandomUserAgent(): String {
    val osPlatforms = listOf(
        "Windows NT ${Random.nextInt(6, 11)}.0; Win64; x64",
        "Macintosh; Intel Mac OS X 10_${Random.nextInt(8, 16)}_${Random.nextInt(0, 8)}",
        "X11; Ubuntu; Linux x86_64",
        "Linux; Android ${Random.nextInt(7, 12)}",
        "iPhone; CPU iPhone OS ${Random.nextInt(10, 15)}_${Random.nextInt(0, 6)} like Mac OS X"
    )

    val browsers = listOf(
        "Chrome/${Random.nextInt(120, 127)}.0.${Random.nextInt(4000, 5000)}.${Random.nextInt(100, 200)}",
        "Firefox/${Random.nextInt(120, 129)}.0",
        "Safari/605.1.${Random.nextInt(10, 30)}",
        "Edge/${Random.nextInt(16, 20)}.${Random.nextInt(10000, 20000)}"
    )

    val webKits = listOf(
        "AppleWebKit/${Random.nextInt(537, 540)}.36 (KHTML, like Gecko)",
        "AppleWebKit/605.1.${Random.nextInt(10, 30)} (KHTML, like Gecko)"
    )

    val platform = osPlatforms.random()
    val browser = browsers.random()
    val webKit = if (browser.contains("Chrome") || browser.contains("Safari") || browser.contains("Edge")) webKits.random() else ""

    return when {
        browser.contains("Chrome") -> "Mozilla/5.0 ($platform) $webKit $browser Safari/537.36"
        browser.contains("Safari") -> "Mozilla/5.0 ($platform) $webKit $browser"
        browser.contains("Firefox") -> "Mozilla/5.0 ($platform; rv:${browser.split("/")[1]}) Gecko/20100101 $browser"
        browser.contains("Edge") -> "Mozilla/5.0 ($platform) $webKit Edge/${browser.split("/")[1]}"
        else -> "Mozilla/5.0 ($platform)"
    }
}

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

fun OkHttpClient.request(
    url: URL,
    method: HttpMethod = HttpMethod.GET,
    body: RequestBody? = null,
    headers: Headers = Headers.headersOf(),
    userAgent: String = USER_AGENT,
): Call = request(
    url = url.toString(),
    method = method,
    body = body,
    headers = headers,
    userAgent = userAgent
)

fun OkHttpClient.request(
    url: HttpUrl,
    method: HttpMethod = HttpMethod.GET,
    body: RequestBody? = null,
    headers: Headers = Headers.headersOf(),
    userAgent: String = USER_AGENT,
): Call = request(
    url = url.toString(),
    method = method,
    body = body,
    headers = headers,
    userAgent = userAgent
)