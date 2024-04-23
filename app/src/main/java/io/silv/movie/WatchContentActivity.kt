package io.silv.movie

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
import androidx.compose.ui.Modifier
import io.silv.core_ui.components.NoResultsEmptyScreen
import io.silv.core_ui.theme.MovieTheme


class WatchContentActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val url = intent.getStringExtra("url")

        setContent {

            BackHandler {
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }

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