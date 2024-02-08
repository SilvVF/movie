@file:OptIn(ExperimentalMaterial3Api::class)

package io.silv.movie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import io.silv.core_ui.theme.MovieTheme
import io.silv.movie.presentation.movie.MovieScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MovieTheme {
                Surface(Modifier.fillMaxSize()) {
                    Scaffold(
                        contentWindowInsets = WindowInsets(0),
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            BottomAppBar(
                                actions = {
                                    IconButton(onClick = { /*TODO*/ }) {
                                        Icon(
                                            imageVector = Icons.Filled.Home,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        Box(
                            Modifier
                                .padding(paddingValues)
                                .consumeWindowInsets(paddingValues)
                        ) {
                            Navigator(MovieScreen()) { navigator ->
                                FadeTransition(navigator)
                            }
                        }
                    }
                }
            }
        }
    }
}


