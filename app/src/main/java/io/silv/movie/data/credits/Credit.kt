package io.silv.movie.data.credits

data class Credit(
    val id: Long,
    val adult: Boolean,
    val gender: Long,
    val knownForDepartment: String,
    val name: String,
    val originalName: String,
    val popularity: Double,
    val profilePath: String?,
    val character: String,
    val creditId: String,
    val crew: Boolean,
    val order: Long?
)

fun io.silv.movie.core.SCredit.toDomain(): Credit {
    return Credit(
        id = id,
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
        order = order
    )
}

data class CreditUpdate(
    val id: Long,
    val adult: Boolean? = null,
    val gender: Long?,
    val knownForDepartment: String?= null,
    val name: String?= null,
    val originalName: String?= null,
    val popularity: Double?= null,
    val profilePath: String?= null,
    val character: String?= null,
    val creditId: String?= null,
    val crew: Boolean?= null,
    val order: Long? = null,
)

fun Credit.toCreditUpdate(): CreditUpdate {
    return CreditUpdate(
        id = id,
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
        order = order
    )
}

