package io.silv.movie.core

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toBitmap
import coil.drawable.ScaleDrawable
import io.silv.movie.R
import java.io.InputStream
import java.io.Serializable
import java.net.URLConnection

object ImageUtil {


    enum class ImageType(val mime: String, val extension: String) {
        AVIF("image/avif", "avif"),
        GIF("image/gif", "gif"),
        HEIF("image/heif", "heif"),
        JPEG("image/jpeg", "jpg"),
        JXL("image/jxl", "jxl"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp"),
    }

    fun findImageType(openStream: () -> InputStream): ImageType? {
        return openStream().use { findImageType(it) }
    }

    private fun findImageType(stream: InputStream): ImageType? {
        return try {
            when (getImageType(stream)) {
                ImageType.AVIF.mime -> ImageType.AVIF
                ImageType.GIF.mime -> ImageType.GIF
                ImageType.HEIF.mime -> ImageType.HEIF
                ImageType.JPEG.mime -> ImageType.JPEG
                ImageType.JXL.mime -> ImageType.JXL
                ImageType.PNG.mime -> ImageType.PNG
                ImageType.WEBP.mime -> ImageType.WEBP
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }


    private fun getImageType(stream: InputStream): String? {
        return URLConnection.guessContentTypeFromStream(stream)
    }

    fun getBitmapOrNull(drawable: Drawable?): Bitmap? = when (drawable) {
        is BitmapDrawable -> drawable.bitmap
        is ScaleDrawable -> drawable.child.toBitmap()
        else -> null
    }

    fun toShareIntent(context: Context, uri: Uri, type: String = "image/*", message: String? = null): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            when (uri.scheme) {
                "http", "https" -> {
                    putExtra(Intent.EXTRA_TEXT, uri.toString())
                }

                "content" -> {
                    message?.let { putExtra(Intent.EXTRA_TEXT, it) }
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
            }
            clipData = ClipData.newRawUri(null, uri)
            setType(type)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        return Intent.createChooser(shareIntent, context.getString(R.string.share))
            .apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
    }

    inline fun <reified T> Intent.getParcelableExtraCompat(name: String): T? {
        return IntentCompat.getParcelableExtra(this, name, T::class.java)
    }

    inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(name: String): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSerializableExtra(name, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getSerializableExtra(name) as? T
        }
    }
}

