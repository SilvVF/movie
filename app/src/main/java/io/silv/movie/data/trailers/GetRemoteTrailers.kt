package io.silv.movie.data.trailers

import io.silv.movie.core.await
import io.silv.movie.network.model.toSTrailer
import io.silv.movie.network.service.tmdb.TMDBMovieService

class GetRemoteTrailers(
    private val tmdbMovieService: TMDBMovieService
) {

    suspend fun await(movieId: Long): List<io.silv.movie.core.STrailer> {
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
}

class GetMovieTrailers(
    private val trailerRepository: TrailerRepository
) {

    suspend fun await(movieId: Long): List<Trailer> {
        return trailerRepository.getTrailersByMovieId(movieId)
    }
}