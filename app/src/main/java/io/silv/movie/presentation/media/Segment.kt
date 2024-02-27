package io.silv.movie.presentation.media

import android.content.Context
import android.os.Build
import androidx.collection.FloatFloatPair
import androidx.core.content.ContextCompat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class SbSkipOptions {
    OFF,
    VISIBLE,
    MANUAL,
    AUTOMATIC,
    AUTOMATIC_ONCE
}

@Serializable
data class Segment(
    @SerialName("UUID") val uuid: String? = null,
    val actionType: String? = null,
    val category: String? = null,
    val description: String? = null,
    val locked: Int? = null,
    private val segment: List<Float> = listOf(),
    val userID: String? = null,
    val videoDuration: Double? = null,
    val votes: Int? = null,
    var skipped: Boolean = false
) {
    @Transient
    val segmentStartAndEnd = FloatFloatPair(segment[0], segment[1])
}

fun Context.supportsHdr(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val display = ContextCompat.getDisplayOrDefault(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            display.isHdr
        } else {
            @Suppress("DEPRECATION")
            display.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() ?: false
        }
    } else {
        false
    }
}