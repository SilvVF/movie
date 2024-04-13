package io.silv.movie.data.movie

import io.silv.movie.core.Status
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.movie.model.MoviePoster

object MovieMapper {

    val mapMovie =
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
          productionCompanies: List<String>?,
          last_modified_at: Long,
          favorite_modified_at: Long?->
            Movie(
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
                productionCompanies = productionCompanies,
                lastModifiedAt = last_modified_at,
                favoriteModifiedAt = favorite_modified_at ?: -1L,
                inLists = inLists.toInt()
            )
        }

    val mapMoviePoster =
        { id: Long,
          title: String,
          poster_url: String?,
          poster_last_updated: Long,
          favorite: Boolean,
          last_modified_at: Long,
          popularity: Double,
          inLists: Long,
          overview: String,
         ->
            MoviePoster(
                id = id,
                title = title,
                posterUrl  = poster_url,
                favorite = favorite,
                posterLastUpdated = poster_last_updated,
                overview = overview,
                inLibraryLists = inLists
            )
        }
}




