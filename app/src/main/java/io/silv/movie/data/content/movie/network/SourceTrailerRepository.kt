package io.silv.movie.data.content.movie.network

import io.silv.movie.core.STrailer
import io.silv.movie.core.await
import io.silv.movie.data.content.movie.local.TrailerRepository
import io.silv.movie.data.content.movie.model.Trailer
import io.silv.movie.network.model.toSTrailer
import io.silv.movie.network.service.tmdb.TMDBMovieService
import io.silv.movie.network.service.tmdb.TMDBTVShowService

class SourceTrailerRepository(
    private val movieService: TMDBMovieService,
    private val showService: TMDBTVShowService
) {

    suspend fun awaitMovie(movieId: Long): List<STrailer> {
        return runCatching {
            movieService.videos(movieId).await()
                .body()!!
                .results
                .map { result ->
                    result.toSTrailer()
                }
        }
            .getOrDefault(emptyList())
    }

    suspend fun awaitShow(showId: Long): List<STrailer> {
        return runCatching {
            showService.videos(showId).await()
                .body()!!
                .results
                .map { result ->
                    result.toSTrailer()
                }
        }
            .getOrDefault(emptyList())
    }
}

suspend fun TrailerRepository.networkToLocalTrailer(
    trailer: Trailer,
    contentId: Long,
    isMovie: Boolean
) = when (val local = getById(trailer.id)) {
    null -> {
        insertTrailer(trailer, contentId, isMovie)
        trailer
    }

    else -> local
}
