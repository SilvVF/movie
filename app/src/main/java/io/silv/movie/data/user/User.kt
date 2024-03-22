package io.silv.movie.data.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("user_id")
    val userId: String,
    val email: String,
    val username: String,
    @SerialName("genre_ratings")
    val genreRatings: String? = null,
    @SerialName("profile_image")
    val profileImage: String? = null,
    @SerialName("favorites_public")
    val favoritesPublic: Boolean
)
