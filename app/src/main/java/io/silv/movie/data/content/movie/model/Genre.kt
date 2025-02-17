package io.silv.movie.data.content.movie.model

import android.os.Parcelable
import androidx.compose.runtime.Stable
import io.silv.movie.core.SGenre
import kotlinx.parcelize.Parcelize

@Stable
@Parcelize
data class Genre(
    val name: String,
    val id: Long? = null
): Parcelable

fun SGenre.toDomain(): Genre {
    return Genre(name, id)
}
