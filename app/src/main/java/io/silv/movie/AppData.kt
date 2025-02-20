package io.silv.movie

import android.text.format.DateUtils
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import io.silv.movie.prefrences.AppTheme
import io.silv.movie.prefrences.ThemeMode
import io.silv.movie.prefrences.UiPreferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
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

class AppStateProvider(
    private val uiPreferences: UiPreferences,
) {
    val observeAppData = combine(
        uiPreferences.themeMode().changes(),
        uiPreferences.appTheme().changes(),
        uiPreferences.dateFormat().changes(),
        uiPreferences.themeDarkAmoled().changes(),
        uiPreferences.relativeTime().changes(),
        uiPreferences.sharedElementTransitions().changes(),
        uiPreferences.predictiveBack().changes(),
    ) { arr ->
        AppState.Success(
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
}

@Stable
sealed interface AppState {
    @Stable
    data object Loading : AppState

    @Stable
    data class Success(val state: AppData) : AppState

    val success: Success? get() = this as? Success
}