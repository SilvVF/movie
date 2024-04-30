package io.silv.movie

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.Navigator

object Nav {

    var current by mutableStateOf<Navigator?>(null)
        private set

    fun setNav(navigator: Navigator) {
        current = navigator
    }

    fun clear() { current = null }
}