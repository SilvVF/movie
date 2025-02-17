package io.silv.movie.data.network

import io.silv.movie.IoDispatcher
import io.silv.movie.core.STrailer
import io.silv.movie.core.await
import io.silv.movie.core.suspendRunCatching
import io.silv.movie.data.local.TrailerRepository
import io.silv.movie.data.model.Trailer
import io.silv.movie.network.model.toSTrailer
import io.silv.movie.network.service.tmdb.TMDBMovieService
import io.silv.movie.network.service.tmdb.TMDBTVShowService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class SourceTrailerRepository(
    private val movieService: TMDBMovieService,
    private val showService: TMDBTVShowService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun awaitMovie(movieId: Long): Result<List<STrailer>> = withContext(ioDispatcher) {
        suspendRunCatching {
            movieService.videos(movieId).await()
                .body()!!
                .results
                .map { result ->
                    result.toSTrailer()
                }
        }
    }

    suspend fun awaitShow(showId: Long): Result<List<STrailer>> = withContext(ioDispatcher) {
        suspendRunCatching {
            showService.videos(showId).await()
                .body()!!
                .results
                .map { result ->
                    result.toSTrailer()
                }
        }
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
