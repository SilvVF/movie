package io.silv.movie.presentation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.silv.movie.data.prefrences.core.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class PreferenceMutableState<T>(
    private val preference: Preference<T>,
    private val scope: CoroutineScope,
) : MutableState<T> {

    private val state = mutableStateOf(
        runBlocking {
            withTimeoutOrNull(500) {
                preference.get()
            }
                ?: preference.defaultValue()
        }
    )

    init {
        preference.changes()
            .onEach { state.value = it }
            .launchIn(scope)
    }

    override var value: T
        get() = state.value
        set(value) {
            scope.launch { preference.set(value) }
        }

    override fun component1(): T {
        return state.value
    }

    override fun component2(): (T) -> Unit {
        return { scope.launch { preference.set(it) } }
    }
}

fun <T> Preference<T>.asState(scope: CoroutineScope) = PreferenceMutableState(this, scope)