package io.silv.movie.data.credits

object CreditsMapper {

    val mapCredit = {
            id: Long,
            adult: Boolean,
            gender: Long,
            known_for_department: String,
            name: String,
            original_name: String,
            popularity: Double,
            profile_path: String?,
            character: String,
            credit_id: String,
            crew: Boolean,
            ordering: Long? ->
        Credit(
            id = id,
            adult = adult,
            gender = gender,
            knownForDepartment = known_for_department,
            name = name,
            originalName = original_name,
            popularity = popularity,
            profilePath = profile_path,
            character = character,
            creditId = credit_id,
            crew = crew,
            order = ordering
        )
    }
}