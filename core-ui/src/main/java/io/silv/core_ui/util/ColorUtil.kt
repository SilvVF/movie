package io.silv.core_ui.util

import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

@Composable
fun rememberDominantColor(
    data: Any?,
    fallback: Color = MaterialTheme.colorScheme.primary
): State<Color> {


    val context = LocalContext.current
    val color = remember {
        mutableStateOf(Color.Transparent)
    }

    LaunchedEffect(data, fallback) {
        runCatching {

            if (data == null)
                error("null data")

            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(data)
                .allowHardware(false) // Disable hardware bitmaps.
                .build()

            val result = (loader.execute(request) as SuccessResult).drawable
            val bitmap = (result as BitmapDrawable).bitmap
            val palette = Palette.from(bitmap).generate()
            val dominant = palette.getDominantColor(color.value.toArgb())

            color.value = Color(dominant)
        }
            .onFailure {
                color.value = fallback
            }
    }

    return color
}

private fun mutableColorStateSaver() = Saver<MutableState<Color>, Long>(
    save = { state ->  state.value.value.toLong() },
    restore = { value -> mutableStateOf(Color(value.toULong())) },
)

