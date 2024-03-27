package io.silv.movie.data.credits

import io.silv.core_ui.components.PosterData


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
            profile_path: String? ->
        Credit(
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
            personId = person_id
        )
    }

    val mapCreditWithPoster = {
            credit_id: String, movie_id: Long?, show_id: Long?, adult: Boolean,
            gender: Long, known_for_department: String, name: String,
            original_name: String, popularity: Double, character: String,
            crew: Boolean, ordering: Long?, person_id: Long?, profile_path: String?,
            contentId: Long?, title: String?, posterUrl: String?,
            posterLastUpdated: Long?, favorite: Boolean?, inLibraryLists: Long?

            ->
        CreditWithPoster(
            Credit(
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
                personId = person_id
            ),
            PosterData(
                movie_id ?: show_id!!,
                posterUrl,
                title.orEmpty(),
                favorite != false,
                movie_id != null,
                posterLastUpdated ?: -1L,
                (inLibraryLists ?: -1) >= 1,
            )
        )
    }
}