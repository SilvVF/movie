package io.silv.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okio.IOException
import kotlin.coroutines.resumeWithException

// Based on https://github.com/gildor/kotlin-coroutines-okhttp
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun Call.await(callStack: Array<StackTraceElement>): Response {
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


// Based on https://github.com/gildor/kotlin-coroutines-okhttp
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> retrofit2.Call<T>.await(callStack: Array<StackTraceElement>): retrofit2.Response<T> {
    return suspendCancellableCoroutine { continuation ->

        val callback =
            object : retrofit2.Callback<T> {
                override fun onResponse(call: retrofit2.Call<T>, response: retrofit2.Response<T>) {
                    continuation.resume(response) {
                        response.raw().body?.close()
                    }
                }

                override fun onFailure(call: retrofit2.Call<T>, t: Throwable) {
                    // Don't bother with resuming the continuation if it is already cancelled.
                    if (continuation.isCancelled) return
                    val exception = IOException(t.message, t).apply { stackTrace = callStack }
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

suspend fun <T> retrofit2.Call<T>.await(): retrofit2.Response<T> {
    val callStack = Exception().stackTrace.run { copyOfRange(1, size) }
    return await(callStack)
}