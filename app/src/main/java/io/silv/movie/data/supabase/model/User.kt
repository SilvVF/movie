package io.silv.movie.data.supabase.model

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
    @SerialName("profile_image")
    val profileImage: String? = null,
    @SerialName("favorites_public")
    val favoritesPublic: Boolean
) {

    companion object {

        fun default(): User {
            return User(
                "c532e5da-71ca-4b4b-b896-d1d36f335149",
                "defaultmovielist@gmail.com",
                "defaultmovielist",
                "DragonBall/dbz_007.jpg",
                true
            )
        }

        object Serializer {
            fun deserialize(serialized: String): User? {
                return Companion.deserialize(serialized)
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
