package io.silv.core_ui.voyager

import android.os.Parcelable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.core.lifecycle.DisposableEffectIgnoringConfiguration
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set


@Composable
inline fun <reified T : ScreenResult> rememberScreenResult(
    resultKey: ScreenKey
): State<T?> {
    val screenResult = remember { mutableStateOf<T?>(null) }

    LaunchedEffect(resultKey) {
        screenResults.collect {
            val res = it[resultKey] as? T
            if (res != null) {
                screenResult.value = res
            }
        }
    }

    return screenResult
}

@Composable
inline fun <S, reified R : ScreenResult> rememberScreenWithResultLauncher(
    screen: S,
    navigator: Navigator = LocalNavigator.currentOrThrow,
    noinline onResult: (R) -> Unit = {}
): ScreenWithResultLauncher<S, R> where  S : Screen, S : ScreenWithResult<R> {

    val screenResultLauncher = remember(screen, navigator) {
        ScreenWithResultLauncher(
            screen,
            navigator
        )
    }

    TriggerOnScreenResultEffect(
        key = screenResultLauncher.screen.key,
        onResult = onResult
    )

    return screenResultLauncher
}

fun interface GlobalNavigator {
    suspend fun navigate(event: Navigator.() -> Unit)
}

class ScreenResultsViewModel(
    private val state: SavedStateHandle
) : ViewModel(), GlobalNavigator {

    private val screenSendCh = Channel<Navigator.() -> Unit>(UNLIMITED)
    val screenNavCh = screenSendCh.receiveAsFlow().produceIn(viewModelScope)

    override suspend fun navigate(event: Navigator.() -> Unit) =
        withContext(Dispatchers.Main.immediate) {
            screenSendCh.send(event)
        }

    fun bind() {
        ScreenResultsStoreProxy.screenResultModel = this
    }

    internal val screenResults = MutableStateFlow(
        state.get<Map<ScreenKey, ScreenResult>>(SCREEN_RESULTS_SAVEDSTATE_KEY).orEmpty()
    )

    private val callbacks = mutableMapOf<ScreenKey, (ScreenResult) -> Unit>()

    @Synchronized
    internal fun registerCallback(
        key: ScreenKey,
        onResult: (ScreenResult) -> Unit
    ) {
        callbacks[key] = onResult
    }

    @Synchronized
    internal fun unregisterCallback(
        key: ScreenKey,
        onResult: (ScreenResult) -> Unit
    ) {
        if (callbacks[key] == onResult) {
            callbacks.remove(key)
        }
    }


    @Synchronized
    internal fun <T : ScreenResult> setResult(screenResultSourceKey: ScreenKey, result: T) {

        val callback = callbacks.remove(screenResultSourceKey)
        callback?.invoke(result)

        Log.d("d", "Callback $callback")

        screenResults.update { results ->
            results.toMutableMap().apply {
                this[screenResultSourceKey] = result
            }
        }
    }

    @Synchronized
    internal fun removeResult(screenResultSourceKey: ScreenKey) {
        screenResults.update { results ->
            results.toMutableMap().apply {
                this.remove(screenResultSourceKey)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        state[SCREEN_RESULTS_SAVEDSTATE_KEY] = screenResults.value
    }
}

fun triggerOnScreenResultEffect(key: ScreenKey, callback: (ScreenResult) -> Unit) {
    ScreenResultsStoreProxy.screenResultModel.registerCallback(key, callback)
}

fun disposeOnScreenResultEffect(key: ScreenKey, callback: (ScreenResult) -> Unit) {
    ScreenResultsStoreProxy.screenResultModel.unregisterCallback(key, callback)
}

@Composable
inline fun <reified R: ScreenResult> TriggerOnScreenResultEffect(
    key: ScreenKey,
    noinline onResult: (R) -> Unit = {}
) {
    val resultCall by rememberUpdatedState(newValue = onResult)

    DisposableEffectIgnoringConfiguration(key) {
        triggerOnScreenResultEffect(key) { result ->
            (result as R?)?.let(resultCall)
        }
        onDispose {
            @Suppress("UNCHECKED_CAST")
            disposeOnScreenResultEffect(key, (resultCall as? (ScreenResult) -> Unit) ?: {})
        }
    }
}

private object ScreenResultsStoreProxy {

    lateinit var screenResultModel: ScreenResultsViewModel

    fun <T : ScreenResult> setResult(screenResultSourceKey: ScreenKey, result: T) =
        screenResultModel.setResult(screenResultSourceKey, result)

    fun removeResult(screenResultSourceKey: ScreenKey) =
        screenResultModel.removeResult(screenResultSourceKey)
}

class ScreenWithResultLauncher<S, R : ScreenResult>(
    val screen: S,
    private val navigator: Navigator,
) where S : Screen, S : ScreenWithResult<R> {

    fun launch() {
        screen.clearScreenResult()
        navigator.push(screen)
    }
}

fun <T : ScreenResult> ScreenWithResult<T>.setScreenResult(
    result: T,
    screenResultSourceKey: ScreenKey = key
) = ScreenResultsStoreProxy.setResult(screenResultSourceKey, result)

fun <T : ScreenResult> ScreenWithResult<T>.clearScreenResult(
    screenResultSourceKey: ScreenKey = key
) = ScreenResultsStoreProxy.removeResult(screenResultSourceKey)

interface ScreenResult : Parcelable

interface ScreenWithResult<T : ScreenResult> : Screen

val screenResults: StateFlow<Map<ScreenKey, ScreenResult>>
    get() = ScreenResultsStoreProxy.screenResultModel.screenResults

private const val SCREEN_RESULTS_SAVEDSTATE_KEY = "screen_results"
