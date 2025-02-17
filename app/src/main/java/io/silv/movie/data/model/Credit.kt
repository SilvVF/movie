package io.silv.movie.data.model


data class Credit(
    val creditId: String,
    val contentId: Long,
    val isMovie: Boolean,
    val adult: Boolean,
    val gender: Long,
    val knownForDepartment: String,
    val name: String,
    val originalName: String,
    val popularity: Double,
    val profilePath: String?,
    val character: String,
    val crew: Boolean,
    val order: Long?,
    val personId: Long?,
    val posterPath: String?,
    val title: String
)

fun io.silv.movie.core.SCredit.toDomain(): Credit {
    return Credit(
        adult = adult,
        gender = gender,
        knownForDepartment = knownForDepartment,
        name = name,
        originalName = originalName,
        popularity = popularity,
        profilePath = profilePath,
        character = character,
        creditId = creditId,
        crew = crew,
        order = order,
        personId = personId,
        posterPath = null,
        contentId = -1,
        isMovie = false,
        title = ""
    )
}

data class CreditUpdate(
    val creditId: String,
    val adult: Boolean? = null,
    val gender: Long?,
    val knownForDepartment: String?= null,
    val name: String?= null,
    val originalName: String?= null,
    val popularity: Double?= null,
    val profilePath: String?= null,
    val character: String?= null,
    val crew: Boolean?= null,
    val title: String? = null,
    val order: Long? = null,
    val personId: Long? = null,
    val posterPath: String?,
)

fun Credit.toCreditUpdate(): CreditUpdate {
    return CreditUpdate(
        creditId = creditId,
        adult = adult,
        gender = gender,
        knownForDepartment = knownForDepartment,
        name = name,
        originalName = originalName,
        popularity = popularity,
        profilePath = profilePath,
        character = character,
        crew = crew,
        order = order,
        personId = personId,
        posterPath = posterPath
    )
}

