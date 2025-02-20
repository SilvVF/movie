package io.silv.movie.presentation.media

import WebViewScreenContent
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.silv.core_ui.components.NoResultsEmptyScreen
import io.silv.movie.AppState
import io.silv.movie.AppStateProvider
import io.silv.movie.MainActivity
import io.silv.movie.MovieTheme
import io.silv.movie.prefrences.UiPreferences
import io.silv.movie.presentation.LocalAppState
import kotlinx.coroutines.channels.Channel
import org.koin.android.ext.android.inject


class WatchContentActivity : ComponentActivity() {

    val uiPreferences by inject<UiPreferences>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val url = intent.getStringExtra("url")
        val appState = AppStateProvider(uiPreferences)

        setContent {

            BackHandler {
                startActivity(
                    Intent(applicationContext, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            }
            val state by appState.observeAppData.collectAsStateWithLifecycle(AppState.Loading)

            when (val s = state) {
                AppState.Loading -> {}
                is AppState.Success -> {
                    CompositionLocalProvider(LocalAppState provides s.state) {
                        MovieTheme {
                            Scaffold { paddingValues ->
                                if (url.isNullOrEmpty()) {
                                    NoResultsEmptyScreen(contentPaddingValues = paddingValues)
                                } else {
                                    Box(Modifier.padding(paddingValues)) {
                                        WebViewScreenContent(
                                            url = url,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}