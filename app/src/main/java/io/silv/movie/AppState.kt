package io.silv.movie

import android.text.format.DateUtils
import androidx.annotation.CallSuper
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import io.silv.movie.data.prefrences.AppTheme
import io.silv.movie.data.prefrences.ThemeMode
import io.silv.movie.data.prefrences.UiPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
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

@Stable
class AppStateProvider(
    private val uiPreferences: UiPreferences,
    scope: CoroutineScope
) {
    var state by mutableStateOf<State>(State.Loading)
        private set

    private val appStateChanges = combine(
        uiPreferences.themeMode().changes(),
        uiPreferences.appTheme().changes(),
        uiPreferences.dateFormat().changes(),
        uiPreferences.themeDarkAmoled().changes(),
        uiPreferences.relativeTime().changes().combine(uiPreferences.sharedElementTransitions().changes()) { a: Boolean, b: Boolean -> a to b },
    ) { themeMode, appTheme, dateFormat, amoled, (relativeTime, transitions) ->
        AppState(
            appTheme,
            themeMode,
            amoled,
            UiPreferences.dateFormat(dateFormat),
            relativeTime,
            (state as? State.Success)?.state?.startScreen
                ?: uiPreferences.startScreen().get().tab,
            transitions
        )
    }

    init {
        scope.launch {
            state = State.Success(
                AppState(
                    appTheme = uiPreferences.appTheme().get(),
                    amoled = uiPreferences.themeDarkAmoled().get(),
                    themeMode = uiPreferences.themeMode().get(),
                    dateFormat = UiPreferences.dateFormat(uiPreferences.dateFormat().get()),
                    startScreen = uiPreferences.startScreen().get().tab,
                    relativeTimestamp = uiPreferences.relativeTime().get(),
                    sharedElementTransitions = uiPreferences.sharedElementTransitions().get()
                )
            )

            appStateChanges.collect {
                state = State.Success(it)
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

abstract class ScopedStateScreenModel<T>(state: T): StateScreenModel<T>(state), KoinScopeComponent {

    override val scope: Scope by lazy { createScope(this) }

    // clear scope
    @CallSuper
    override fun onDispose() {
        super.onDispose()
        scope.close()
    }
}

abstract class ScopedScreenModel: ScreenModel, KoinScopeComponent {

    override val scope: Scope by lazy { createScope(this) }

    // clear scope
    @CallSuper
    override fun onDispose() {
        super.onDispose()
        scope.close()
    }
}