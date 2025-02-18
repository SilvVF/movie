package io.silv.movie.core

import java.io.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

typealias MB = Long

val Long.MB : MB get() = this

fun MB.toBytes(): Long = this * 1000L

data class Quad<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) : Serializable {

    /**
     * Returns string representation of the [Quad] including its [first], [second], [third] and [fourth] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth)"
}

data class Penta<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
) : Serializable {

    /**
     * Returns string representation of the [Quad] including its [first], [second], [third], [fourth] and [fifth] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth)"
}

/**
 * Attempts [block], returning a successful [Result] if it succeeds, otherwise a [Result.Failure]
 * taking care not to break structured concurrency
 */
suspend fun <T> suspendRunCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancellationException: CancellationException) {
        throw cancellationException
    } catch (exception: Exception) {
        Result.failure(exception)
    }

@OptIn(ExperimentalContracts::class)
fun <T, R> Iterable<T>.filterUniqueBy(selector: (T) -> R): List<T> {
    contract { callsInPlace(selector, InvocationKind.UNKNOWN) }
    val seen = mutableSetOf<R>()
    return buildList {
        for (e in this@filterUniqueBy) {
            if (seen.add(selector(e))) {
                add(e)
            }
        }
    }
}