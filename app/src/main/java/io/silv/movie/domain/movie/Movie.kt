package io.silv.movie.domain.movie

import androidx.compose.runtime.Stable

@Stable
data class Movie(
    val id: Int,
    val title: String,
    val posterUrl: String,
)
