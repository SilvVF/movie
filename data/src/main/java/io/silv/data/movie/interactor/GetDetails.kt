package io.silv.data.movie.interactor

import androidx.compose.runtime.Stable
import io.silv.core.await
import io.silv.core_network.TMDBMovieService
import io.silv.core_network.model.movie.MovieDetailsResponse
import io.silv.core_network.model.movie.MovieVideoResponse
import io.silv.data.movie.model.Genre

class GetMovieDetails(
    private val tmdbMovieService: TMDBMovieService,
) {

    suspend fun await(id: Long): Result<MovieDetails> {
        return runCatching {
            tmdbMovieService
                .details(id)
                .await()
                .body()!!
                .toDomain()
        }
    }

    suspend fun awaitVideos(id: Long): Result<List<MovieVideo>> {
        return runCatching {
            tmdbMovieService.videos(id)
                .await()
                .body()!!
                .results.map { it.toDomain() }
        }
    }
}

fun MovieVideoResponse.Result.toDomain(): MovieVideo {
    val it = this
    return MovieVideo(
        it.id,
        it.iso31661,
        it.iso6391,
        it.key,
        it.name,
        it.official,
        it.publishedAt,
        it.site,
        it.size,
        it.type
    )
}

fun MovieDetailsResponse.toDomain(): MovieDetails {
    return MovieDetails(
        adult = adult,
        backdropPath = backdropPath,
        budget = budget,
        genres = genres.map { Genre(it.name, it.id.toLong()) },
        homepage = homepage,
        id = id,
        imdbId = imdbId,
        originalLanguage = originalLanguage,
        originalTitle = originalTitle,
        overview = overview,
        popularity = popularity,
        posterPath = posterPath,
        releaseDate = releaseDate,
        revenue = revenue,
        runtime = runtime,
        status = status,
        tagline = tagline,
        voteAverage = voteAverage,
        voteCount = voteCount,
        title = title,
        productionCompanies = productionCompanies.map { it.name }
    )
}

@Stable
data class MovieVideo(
    val id: String,
    val iso31661: String,
    val iso6391: String,
    val key: String,
    val name: String,
    val official: Boolean,
    val publishedAt: String,
    val site: String,
    val size: Int,
    val type: String
)

@Stable
data class MovieDetails(
    val adult: Boolean,
    val backdropPath: String,
    val budget: Int = 0,
    val genres: List<Genre> = listOf(),
    val productionCompanies: List<String>,
    val homepage: String = "",
    val id: Int = 0,
    val imdbId: String = "",
    val originalLanguage: String = "",
    val originalTitle: String = "",
    val overview: String = "",
    val popularity: Double = 0.0,
    val posterPath: String = "",
    val releaseDate: String = "",
    val revenue: Int = 0,
    val runtime: Int = 0,
    val status: String = "",
    val tagline: String = "",
    val title: String = "",
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0
)