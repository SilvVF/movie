package io.silv.movie.data.movie

import io.silv.movie.core.Status
import io.silv.movie.data.movie.model.Movie

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
          externalUrl: String,
          popularity: Double,
          status: Long?  ->
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
                status = status?.let { Status.entries[status.toInt()] }
            )
        }
}




