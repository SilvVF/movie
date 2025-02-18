package io.silv.movie

import android.text.format.DateUtils
import androidx.annotation.CallSuper
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import io.silv.movie.prefrences.AppTheme
import io.silv.movie.prefrences.ThemeMode
import io.silv.movie.prefrences.UiPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.scope.Scope
import java.text.DateFormat
import java.util.Date

@Stable
@Immutable
data class AppState(
    val appTheme: AppTheme,
    val themeMode: ThemeMode,
    val amoled: Boolean,
    val dateFormat: DateFormat,
    val relativeTimestamp: Boolean,
    val startScreen: Tab,
    val sharedElementTransitions: Boolean,
    val predictiveBackNavigation: Boolean,
) {

    fun formatDate(i: Instant): String {
        return if (relativeTimestamp) {
            DateUtils.getRelativeTimeSpanString(
                i.toEpochMilliseconds(),
                Clock.System.now().toEpochMilliseconds(),
                DateUtils.MINUTE_IN_MILLIS
            )
                .toString()
        } else {
            dateFormat.format(Date.from(i.toJavaInstant()))
        }
    }
}

class AppStateProvider(
    private val uiPreferences: UiPreferences,
    scope: LifecycleCoroutineScope,
    private val lifecycle: Lifecycle,
) {
    private val _state = MutableStateFlow<State>(State.Loading)
    val state = _state.asStateFlow()

    private val appStateChanges = combine(
        uiPreferences.themeMode().changes(),
        uiPreferences.appTheme().changes(),
        uiPreferences.dateFormat().changes(),
        uiPreferences.themeDarkAmoled().changes(),
        uiPreferences.relativeTime().changes(),
        uiPreferences.sharedElementTransitions().changes(),
        uiPreferences.predictiveBack().changes(),
    ) { arr ->
        State.Success(
            AppState(
                themeMode = arr[0] as ThemeMode,
                appTheme = arr[1] as AppTheme,
                dateFormat = UiPreferences.dateFormat(arr[2] as String),
                amoled = arr[3] as Boolean,
                relativeTimestamp = arr[4] as Boolean,
                sharedElementTransitions = arr[5] as Boolean,
                predictiveBackNavigation = arr[6] as Boolean,
                startScreen = uiPreferences.startScreen().get().tab
            )
        )
    }
        .onStart {
            emit(
                State.Success(
                    AppState(
                        appTheme = uiPreferences.appTheme().get(),
                        amoled = uiPreferences.themeDarkAmoled().get(),
                        themeMode = uiPreferences.themeMode().get(),
                        dateFormat = UiPreferences.dateFormat(uiPreferences.dateFormat().get()),
                        startScreen = uiPreferences.startScreen().get().tab,
                        relativeTimestamp = uiPreferences.relativeTime().get(),
                        sharedElementTransitions = uiPreferences.sharedElementTransitions().get(),
                        predictiveBackNavigation = uiPreferences.predictiveBack().get()
                    )
                )
            )
        }

    init {
        scope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appStateChanges.collect {
                    _state.emit(it)
                }
            }
        }
    }


    @Stable
    sealed interface State {
        @Stable
        data object Loading: State
        @Stable
        data class Success(val state: AppState): State
    }
}
