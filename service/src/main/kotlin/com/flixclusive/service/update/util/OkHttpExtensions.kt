package com.flixclusive.service.update.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import kotlin.coroutines.resumeWithException

// Based on https://github.com/gildor/kotlin-coroutines-okhttp
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun Call.await(callStack: Array<StackTraceElement>): Response {
    return suspendCancellableCoroutine { continuation ->
        val callback =
            object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response) {
                        response.body?.close()
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    // Don't bother with resuming the continuation if it is already cancelled.
                    if (continuation.isCancelled) return
                    val exception = IOException(e.message, e).apply { stackTrace = callStack }
                    continuation.resumeWithException(exception)
                }
            }

        enqueue(callback)

        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (ex: Throwable) {
                // Ignore cancel exception
            }
        }
    }
}

suspend fun Call.await(): Response {
    val callStack = Exception().stackTrace.run { copyOfRange(1, size) }
    return await(callStack)
}


fun OkHttpClient.newCachelessCallWithProgress(request: Request, listener: ProgressListener): Call {
    val progressClient = newBuilder()
        .cache(null)
        .followRedirects(true)
        .followSslRedirects(true)
        .addNetworkInterceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            originalResponse.body?.let {
                originalResponse.newBuilder()
                    .body(ProgressResponseBody(it, listener))
                    .build()
            } ?: originalResponse
        }
        .build()

    return progressClient.newCall(request)
}

