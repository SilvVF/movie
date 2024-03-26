package io.silv.movie.network.model.credits


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreditsResponse(
    @SerialName("cast")
    val cast: List<Cast> = listOf(),
    @SerialName("crew")
    val crew: List<Crew> = listOf(),
    @SerialName("id")
    val id: Int = 0
) {
    @Serializable
    data class Cast(
        @SerialName("adult")
        val adult: Boolean = false,
        @SerialName("cast_id")
        val castId: Int = 0,
        @SerialName("character")
        val character: String = "",
        @SerialName("credit_id")
        val creditId: String = "",
        @SerialName("gender")
        val gender: Long = 0,
        @SerialName("id")
        val id: Long = 0,
        @SerialName("known_for_department")
        val knownForDepartment: String = "",
        @SerialName("name")
        val name: String = "",
        @SerialName("order")
        val order: Int = 0,
        @SerialName("original_name")
        val originalName: String = "",
        @SerialName("popularity")
        val popularity: Double = 0.0,
        @SerialName("profile_path")
        val profilePath: String? = ""
    )

    @Serializable
    data class Crew(
        @SerialName("adult")
        val adult: Boolean = false,
        @SerialName("credit_id")
        val creditId: String = "",
        @SerialName("department")
        val department: String = "",
        @SerialName("gender")
        val gender: Long = 0,
        @SerialName("id")
        val id: Long = 0,
        @SerialName("job")
        val job: String = "",
        @SerialName("known_for_department")
        val knownForDepartment: String = "",
        @SerialName("name")
        val name: String = "",
        @SerialName("original_name")
        val originalName: String = "",
        @SerialName("popularity")
        val popularity: Double = 0.0,
        @SerialName("profile_path")
        val profilePath: String? = ""
    )
}