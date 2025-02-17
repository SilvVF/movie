package io.silv.movie.data.content.movie.local

import io.silv.movie.core.Status
import io.silv.movie.data.content.movie.model.TVShow
import io.silv.movie.data.content.movie.model.TVShowPoster


object ShowMapper {

    val mapShow =
        {
            id: Long,
            title: String,
            overview: String,
            genres: List<String>?,
            genre_ids: List<Int>?,
            original_language: String?,
            vote_count: Long?,
            release_date: String?,
            poster_url: String?,
            poster_last_updated: Long,
            favorite: Boolean,
            external_url: String?,
            popularity: Double?,
            status: Long?,
            production_companies: List<String>?,
            last_modified_at: Long,
            favorite_modified_at: Long?,
            inLibraryLists: Long ->
            TVShow(
                id = id,
                title = title,
                posterUrl  = poster_url,
                favorite = favorite,
                posterLastUpdated = poster_last_updated,
                externalUrl = external_url.orEmpty(),
                overview = overview,
                genres = genres.orEmpty(),
                genreIds = genre_ids.orEmpty(),
                originalLanguage = original_language.orEmpty(),
                popularity = popularity ?: 0.0,
                voteCount = vote_count?.toInt() ?: 0,
                releaseDate = release_date.orEmpty(),
                status = status?.let { Status.entries[status.toInt()] },
                productionCompanies = production_companies,
                favoriteLastModified = favorite_modified_at ?: -1L,
                lastModifiedAt = last_modified_at,
                inLists = inLibraryLists.toInt()
            )
        }

    val mapShowPoster =
        {
            id: Long,
            title: String,
            poster_url: String?,
            poster_last_updated: Long,
            favorite: Boolean,
            last_modified_at: Long,
            popularity: Double?,
            overview: String,
            inLists: Long
            ->
            TVShowPoster(
                id = id,
                title = title,
                posterUrl  = poster_url,
                favorite = favorite,
                posterLastUpdated = poster_last_updated,
                inLibraryLists = inLists,
                overview = overview
            )
        }
}
