package io.silv.core_ui.util

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import cafe.adriel.voyager.core.lifecycle.DisposableEffectIgnoringConfiguration
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.launch

@Composable
fun rememberDominantColor(
    data: Any?,
    fallback: Color = MaterialTheme.colorScheme.primary
): State<Color> {


    val context = LocalContext.current
    val color = rememberSaveable(
        saver = Saver(
            save = { it.value.toArgb() },
            restore = {
                mutableStateOf(Color(it))
            }
        )
    ) { mutableStateOf(Color.Transparent) }

    val scope = rememberCoroutineScope()

    DisposableEffectIgnoringConfiguration(data) {
        val job = scope.launch {
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
                    Log.e("rememberDominantColor", it.stackTraceToString())
                    color.value = fallback
                }
        }
        onDispose { job.cancel() }
    }

    return color
}


