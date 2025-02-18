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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.silv.core_ui.components.NoResultsEmptyScreen
import io.silv.movie.AppStateProvider
import io.silv.movie.MainActivity
import io.silv.movie.MovieTheme
import io.silv.movie.prefrences.AppTheme
import io.silv.movie.prefrences.UiPreferences
import io.silv.movie.presentation.LocalAppState
import org.koin.android.ext.android.inject


class WatchContentActivity : ComponentActivity() {

    val uiPreferences by inject<UiPreferences>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val url = intent.getStringExtra("url")
        val appState = AppStateProvider(uiPreferences, lifecycleScope, lifecycle)

        setContent {

            BackHandler {
                startActivity(
                    Intent(applicationContext, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            }
            val state by appState.state.collectAsStateWithLifecycle()

            when (val s = state) {
                AppStateProvider.State.Loading -> {}
                is AppStateProvider.State.Success -> {
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