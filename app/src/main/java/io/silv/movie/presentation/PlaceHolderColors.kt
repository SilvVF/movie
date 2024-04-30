package io.silv.movie.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import io.silv.movie.R
import kotlin.random.Random

object PlaceHolderColors {
    val list = listOf(
        R.color.green,
        R.color.green_mid,
        R.color.orange_alternate,
        R.color.orange,
        R.color.blue,
        R.color.blue_mid,
        R.color.pink,
        R.color.pink_high
    )

    @Composable
    fun rememberColorRandom(key: Any?): Color {
        val id = remember(key) {
            val idx = Random(key.hashCode()).nextInt(0, list.size)
            list[idx]
        }

        return colorResource(id = id)
    }
}