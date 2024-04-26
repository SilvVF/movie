package io.silv.movie.data.movie

import io.silv.movie.core.Status
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.movie.model.MoviePoster

object MovieMapper {

    val mapMovie =
        {
            id: Long,
            title: String,
            overview: String,
            genres: List<String>?,
            genreIds: List<Int>?,
            original_language: String?,
            vote_count: Long?,
            release_date: String?,
            poster_url: String?,
            poster_last_updated: Long,
            favorite: Boolean?,
            externalUrl: String?,
            popularity: Double?,
            status: Long?,
            production_companies: List<String>?,
            last_modified_at: Long,
            favorite_modified_at: Long?,
            inLibraryLists: Long ->
            Movie(
                id = id,
                title = title,
                posterUrl  = poster_url,
                favorite = favorite ?: false,
                posterLastUpdated = poster_last_updated,
                externalUrl = externalUrl.orEmpty(),
                overview = overview,
                genres = genres ?: emptyList(),
                genreIds = genreIds ?: emptyList(),
                originalLanguage = original_language.orEmpty(),
                popularity = popularity ?: 0.0,
                voteCount = vote_count?.toInt() ?: 0,
                releaseDate = release_date.orEmpty(),
                status = status?.let { Status.entries[status.toInt()] },
                productionCompanies = production_companies.orEmpty(),
                lastModifiedAt = last_modified_at,
                favoriteModifiedAt = favorite_modified_at ?: -1L,
                inLists = inLibraryLists.toInt()
            )
        }

    val mapMoviePoster =
        {
            id: Long,
            title: String,
            poster_url: String?,
            poster_last_updated: Long?,
            favorite: Boolean?,
            last_modified_at: Long,
            popularity: Double?,
            overview: String,
            inLists: Long ->
            MoviePoster(
                id = id,
                title = title,
                posterUrl  = poster_url,
                favorite = favorite ?: false,
                posterLastUpdated = poster_last_updated ?: -1L,
                overview = overview,
                inLibraryLists = inLists
            )
        }
}




