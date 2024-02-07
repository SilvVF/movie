package io.silv.data

import androidx.compose.runtime.Stable

@Stable
data class Movie(
    val id: Long,
    val title: String,
    val posterUrl: String?,
    val favorite: Boolean
) {

    companion object {
        fun create() = Movie(
            id = -1L,
            posterUrl = "",
            title = "",
            favorite = false
       )
    }
}


