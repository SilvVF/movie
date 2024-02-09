package io.silv.data.movie.model

import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize

@Stable
@Parcelize
data class Genre(
    val name: String,
    val id: Long? = null
): Parcelable