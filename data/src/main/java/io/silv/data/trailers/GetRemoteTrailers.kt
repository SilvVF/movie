package io.silv.data.trailers

import io.silv.core.STrailer
import io.silv.core.await
import io.silv.core_network.TMDBMovieService
import io.silv.core_network.model.toSTrailer

class GetRemoteTrailers(
    private val tmdbMovieService: TMDBMovieService
) {

    suspend fun await(movieId: Long): List<STrailer> {
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