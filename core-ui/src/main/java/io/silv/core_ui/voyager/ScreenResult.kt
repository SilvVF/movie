package io.silv.core_ui.voyager

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Composable
inline fun <reified T: ScreenResult> rememberScreenResult(
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
inline fun <S, reified R: ScreenResult> rememberScreenWithResultLauncher(
    screen: S,
    navigator: Navigator = LocalNavigator.currentOrThrow,
    noinline  onResult: (R) -> Unit = {}
): ScreenWithResultLauncher<S, R> where  S: Screen, S: ScreenWithResult<R> {

    val screenResultLauncher = remember(screen, navigator) {
        ScreenWithResultLauncher(
            screen,
            navigator
        )
    }

    val resultCall by rememberUpdatedState(newValue = onResult)

    LaunchedEffect(screenResultLauncher.screen.key) {

        val result = screenResults
            .map { it[screenResultLauncher.screen.key] as? R }
            .filterNotNull()
            .first()

        resultCall(result)
    }

    return screenResultLauncher
}

class ScreenResultsViewModel(
    private val state: SavedStateHandle
): ViewModel() {

    val screenResults = MutableStateFlow(
        state.get<Map<ScreenKey, ScreenResult>>(SCREEN_RESULTS_SAVEDSTATE_KEY).orEmpty()
    )

    fun <T: ScreenResult> setResult(screenResultSourceKey: ScreenKey, result: T) {
        screenResults.update { results ->
            results.toMutableMap().apply {
                this[screenResultSourceKey] = result
            }
        }
    }

    fun removeResult(screenResultSourceKey: ScreenKey) {
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

object ScreenResultsStoreProxy {

    lateinit var screenResultModel: ScreenResultsViewModel

    fun <T: ScreenResult> setResult(screenResultSourceKey: ScreenKey, result: T) =
        screenResultModel.setResult(screenResultSourceKey, result)

    fun removeResult(screenResultSourceKey: ScreenKey) =
        screenResultModel.removeResult(screenResultSourceKey)
}

class ScreenWithResultLauncher<S, R: ScreenResult>(
    val screen: S,
    private val navigator: Navigator
) where S: Screen, S: ScreenWithResult<R> {
    fun launch() {
        screen.clearScreenResult()
        navigator.push(screen)
    }
}

fun <T: ScreenResult> ScreenWithResult<T>.setScreenResult(
    result: T,
    screenResultSourceKey: ScreenKey = key
) = ScreenResultsStoreProxy.setResult(screenResultSourceKey, result)

fun <T: ScreenResult> ScreenWithResult<T>.clearScreenResult(
    screenResultSourceKey: ScreenKey = key
) = ScreenResultsStoreProxy.removeResult(screenResultSourceKey)

interface ScreenResult: Parcelable

interface ScreenWithResult <T: ScreenResult>: Screen

val screenResults: StateFlow<Map<ScreenKey, ScreenResult>>
    get() = ScreenResultsStoreProxy.screenResultModel.screenResults

private const val SCREEN_RESULTS_SAVEDSTATE_KEY = "screen_results"