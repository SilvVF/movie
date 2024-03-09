package io.silv.movie.data.tv

import io.silv.movie.core.Status


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
          externalUrl: String,
          popularity: Double,
          status: Long?  ->
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
                status = status?.let { Status.entries[status.toInt()] }
            )
        }

    val mapShowPoster =
        { id: Long,
          title: String,
          posterUrl: String?,
          posterLastUpdated: Long,
          favorite: Boolean,
            ->
            TVShowPoster(
                id = id,
                title = title,
                posterUrl  = posterUrl,
                favorite = favorite,
                posterLastUpdated = posterLastUpdated,
            )
        }
}
