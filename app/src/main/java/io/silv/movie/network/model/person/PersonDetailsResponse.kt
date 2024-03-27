package io.silv.movie.network.model.person


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonDetailsResponse(
    @SerialName("adult")
    val adult: Boolean = false,
    @SerialName("also_known_as")
    val alsoKnownAs: List<String>? = listOf(),
    @SerialName("biography")
    val biography: String? = "",
    @SerialName("birthday")
    val birthday: String? = "",
    @SerialName("deathday")
    val deathday: String? = null,
    @SerialName("gender")
    val gender: Int? = 0,
    @SerialName("homepage")
    val homepage: String? = null,
    @SerialName("id")
    val id: Int? = 0,
    @SerialName("imdb_id")
    val imdbId: String? = "",
    @SerialName("known_for_department")
    val knownForDepartment: String? = "",
    @SerialName("name")
    val name: String? = "",
    @SerialName("place_of_birth")
    val placeOfBirth: String? = "",
    @SerialName("popularity")
    val popularity: Double? = 0.0,
    @SerialName("profile_path")
    val profilePath: String? = ""
)