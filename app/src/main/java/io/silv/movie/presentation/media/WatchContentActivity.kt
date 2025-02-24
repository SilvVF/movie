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
import io.silv.core_ui.voyager.ScreenResultsViewModel
import io.silv.movie.AppDataState
import io.silv.movie.MovieAppState
import io.silv.movie.MainActivity
import io.silv.movie.MovieTheme
import io.silv.movie.data.supabase.BackendRepository
import io.silv.movie.prefrences.UiPreferences
import io.silv.movie.presentation.ContentInteractor
import io.silv.movie.presentation.ListInteractor
import io.silv.movie.presentation.LocalAppData
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel


class WatchContentActivity : ComponentActivity() {

    val uiPreferences by inject<UiPreferences>()
    private val screenResultsViewModel by viewModel<ScreenResultsViewModel>()
    private val listInteractor by inject<ListInteractor>()
    private val contentInteractor by inject<ContentInteractor>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val url = intent.getStringExtra("url")
        val appState =
            MovieAppState(
                uiPreferences,
                contentInteractor,
                listInteractor,
                screenResultsViewModel,
                this,
                lifecycleScope
            )

        setContent {

            BackHandler {
                startActivity(
                    Intent(applicationContext, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
            }
            val state by appState.state.collectAsStateWithLifecycle(AppDataState.Loading)

            when (val s = state) {
                AppDataState.Loading -> {}
                is AppDataState.Success -> {
                    CompositionLocalProvider(LocalAppData provides s.state) {
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