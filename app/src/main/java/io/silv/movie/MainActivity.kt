@file:OptIn(ExperimentalMaterial3Api::class)

package io.silv.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.core_ui.theme.MovieTheme
import io.silv.movie.presentation.movie.MovieScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        setContent {
            MovieTheme {
                Navigator(MovieScreen) { navigator ->
                    FadeTransition(navigator)
                }
            }
        }
    }
}


