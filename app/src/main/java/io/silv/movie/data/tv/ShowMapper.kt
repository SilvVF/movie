package io.silv.movie.data.tv

import io.silv.movie.core.Status
import io.silv.movie.data.tv.model.TVShow
import io.silv.movie.data.tv.model.TVShowPoster


object ShowMapper {

    val mapShow =
        { id: Long,
          title: String,
          overview: String,
          genres: List<String>?,
          genreIds: List<Int>?,
          originalLanguage: String,
          voteCount: Long,
          releaseDate: String,
          posterUrl: String?,
          posterLastUpdated: Long,
          favorite: Boolean,
          inLists: Long,
          externalUrl: String,
          popularity: Double,
          status: Long?,
          production_companies: List<String>?,
          last_modified_at: Long,
          favorite_modified_at: Long?->
            TVShow(
                id = id,
                title = title,
                posterUrl  = posterUrl,
                favorite = favorite,
                posterLastUpdated = posterLastUpdated,
                externalUrl = externalUrl,
                overview = overview,
                genres = genres ?: emptyList(),
                genreIds = genreIds ?: emptyList(),
                originalLanguage = originalLanguage,
                popularity = popularity,
                voteCount = voteCount.toInt(),
                releaseDate = releaseDate,
                status = status?.let { Status.entries[status.toInt()] },
                productionCompanies = production_companies,
                favoriteLastModified = favorite_modified_at ?: -1L,
                lastModifiedAt = last_modified_at,
                inLists = inLists.toInt()
            )
        }

    val mapShowPoster =
        { id: Long,
          title: String,
          poster_url: String?,
          poster_last_updated: Long,
          favorite: Boolean,
          last_modified_at: Long,
          popularity: Double,
          inLists: Long,
            ->
            TVShowPoster(
                id = id,
                title = title,
                posterUrl  = poster_url,
                favorite = favorite,
                posterLastUpdated = poster_last_updated,
                inLibraryLists = inLists
            )
        }
}
