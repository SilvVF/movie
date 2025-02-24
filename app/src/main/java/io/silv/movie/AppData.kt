package io.silv.movie

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import io.silv.core_ui.voyager.GlobalNavigator
import io.silv.movie.prefrences.AppTheme
import io.silv.movie.prefrences.ThemeMode
import io.silv.movie.prefrences.UiPreferences
import io.silv.movie.presentation.ContentInteractor
import io.silv.movie.presentation.ListInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.text.DateFormat
import java.util.Date

@Stable
@Immutable
data class AppData(
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


class MovieAppState(
    private val uiPreferences: UiPreferences,
    contentInteractor: ContentInteractor,
    listInteractor: ListInteractor,
    navigator: GlobalNavigator,
    context: Context,
    scope: CoroutineScope
): GlobalNavigator by navigator {
    val snackbarHostState = SnackbarHostState()

    val state = combine(
        uiPreferences.themeMode().changes(),
        uiPreferences.appTheme().changes(),
        uiPreferences.dateFormat().changes(),
        uiPreferences.themeDarkAmoled().changes(),
        uiPreferences.relativeTime().changes(),
        uiPreferences.sharedElementTransitions().changes(),
        uiPreferences.predictiveBack().changes(),
    ) { arr ->
        AppDataState.Success(
            AppData(
                themeMode = arr[0] as ThemeMode,
                appTheme = arr[1] as AppTheme,
                dateFormat = UiPreferences.dateFormat(arr[2] as String),
                amoled = arr[3] as Boolean,
                relativeTimestamp = arr[4] as Boolean,
                sharedElementTransitions = arr[5] as Boolean,
                predictiveBackNavigation = arr[6] as Boolean,
                startScreen = uiPreferences.startScreen().get().tab,
            )
        )
    }
        .stateIn(
            scope,
            SharingStarted.WhileSubscribed(5_000),
            AppDataState.Loading
        )

    init {
        contentInteractor.eventHandler(
            snackbarHostState,
            context,
            ::navigate
        )
            .launchIn(scope)
        listInteractor.eventHandler(
            snackbarHostState,
            context,
            ::navigate
        )
            .launchIn(scope)
    }
}

@Stable
sealed interface AppDataState {
    @Stable
    data object Loading : AppDataState

    @Stable
    data class Success(val state: AppData) : AppDataState

    val success: Success? get() = this as? Success
}