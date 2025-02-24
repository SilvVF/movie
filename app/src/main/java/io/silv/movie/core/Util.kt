package io.silv.movie.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.compose.currentKoinScope
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
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

suspend fun <A, B> List<A>.pmapSupervised(f: suspend (A) -> B) = supervisorScope {

    val jobs = mutableListOf<Job>()
    val new = MutableList<B?>(this@pmapSupervised.size) { null }

    forEachIndexed { i, value ->
        val job = launch {
            suspendRunCatching { f(value) }
                .onSuccess {
                    new[i] = it
                }
        }
        jobs.add(job)
    }

    jobs.joinAll()

    new.toList().filterNotNull()
}

fun <T> Flow<T>.onEachLatest(action: suspend (value: T) -> Unit): Flow<Unit> {
    /*
     * Implementation note:
     * buffer(0) is inserted here to fulfil user's expectations in sequential usages, e.g.:
     * ```
     * flowOf(1, 2, 3).collectLatest {
     *     delay(1)
     *     println(it) // Expect only 3 to be printed
     * }
     * ```
     *
     * It's not the case for intermediate operators which users mostly use for interactive UI,
     * where performance of dispatch is more important.
     */
    return mapLatest(action).buffer(0)
}

@Stable
class StableParametersDefinition(val parametersDefinition: ParametersDefinition?)

@Composable
fun rememberStableParametersDefinition(
    parametersDefinition: ParametersDefinition?
): StableParametersDefinition = remember { StableParametersDefinition(parametersDefinition) }

@Composable
inline fun <reified T : ScreenModel> Screen.koinScreenModel(
    qualifier: Qualifier? = null,
    scope: Scope = currentKoinScope(),
    noinline parameters: ParametersDefinition? = null
): T {
    val st = parameters?.let { rememberStableParametersDefinition(parameters) }
    val tag = remember(qualifier, scope) { qualifier?.value }
    return rememberScreenModel(tag = tag) {
        scope.get(qualifier, st?.parametersDefinition)
    }
}