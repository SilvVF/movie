package io.silv.movie.data.trailers

import io.silv.movie.core.await
import io.silv.movie.network.model.toSTrailer
import io.silv.movie.network.service.tmdb.TMDBMovieService
import io.silv.movie.network.service.tmdb.TMDBTVShowService

class GetRemoteTrailers(
    private val tmdbMovieService: TMDBMovieService,
    private val tmdbtvShowService: TMDBTVShowService
) {

    suspend fun awaitMovie(movieId: Long): List<io.silv.movie.core.STrailer> {
        return runCatching {
            tmdbMovieService.videos(movieId).await()
                .body()!!
                .results
                .map { result ->
                    result.toSTrailer()
                }
        }
            .getOrDefault(emptyList())
    }

    suspend fun awaitShow(showId: Long): List<io.silv.movie.core.STrailer> {
        return runCatching {
            tmdbtvShowService.videos(showId).await()
                .body()!!
                .results
                .map { result ->
                    result.toSTrailer()
                }
        }
            .getOrDefault(emptyList())
    }
}

class GetTVShowTrailers(
    private val trailerRepository: TrailerRepository
) {
    suspend fun await(showId: Long): List<Trailer> {
        return trailerRepository.getTrailersByShowId(showId)
    }
}

class GetMovieTrailers(
    private val trailerRepository: TrailerRepository
) {

    suspend fun await(movieId: Long): List<Trailer> {
        return trailerRepository.getTrailersByMovieId(movieId)
    }
}