package io.silv.movie.data.content.credits


object CreditsMapper {

    val mapCredit = {
            credit_id: String,
            movie_id: Long?,
            show_id: Long?,
            adult: Boolean,
            gender: Long,
            known_for_department: String,
            name: String,
            original_name: String,
            popularity: Double,
            character: String,
            crew: Boolean,
            ordering: Long?,
            person_id: Long?,
            profile_path: String?,
            poster_path: String?,
            title: String->
        io.silv.movie.data.content.credits.Credit(
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
            order = ordering,
            personId = person_id,
            posterPath = poster_path,
            isMovie = movie_id != null,
            contentId = movie_id ?: show_id!!,
            title = title
        )
    }
}