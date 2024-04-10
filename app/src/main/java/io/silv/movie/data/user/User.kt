package io.silv.movie.data.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
) {

    companion object {
        object Serializer {
            fun deserialize(serialized: String): User? {
                return User.deserialize(serialized)
            }
            fun serialize(value: User?): String {
                return value?.serialize() ?: ""
            }
        }


        fun deserialize(serialized: String): User? {
            return runCatching {
                Json.decodeFromString<User>(serialized)
            }
                .getOrNull()
        }
    }

    fun serialize(): String {
        return Json.encodeToString(this)
    }
}
