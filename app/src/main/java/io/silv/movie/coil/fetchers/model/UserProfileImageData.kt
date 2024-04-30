package io.silv.movie.coil.fetchers.model


data class UserProfileImageData(
    val userId: String,
    val isUserMe: Boolean = false,
    val path: String? = null
)

