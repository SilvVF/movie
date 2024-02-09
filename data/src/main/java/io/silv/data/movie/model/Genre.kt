package io.silv.data.movie.model

import androidx.compose.runtime.Stable

@Stable
data class Genre(
    val name: String,
    val id: Long? = null
)