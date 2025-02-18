package io.silv.movie.data.network

import io.silv.movie.IoDispatcher
import io.silv.movie.core.SCredit
import io.silv.movie.core.await
import io.silv.movie.core.suspendRunCatching
import io.silv.movie.api.model.toSCredits
import io.silv.movie.api.service.tmdb.TMDBMovieService
import io.silv.movie.api.service.tmdb.TMDBTVShowService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext


class SourceCreditsRepository(
    private val tmdbMovieService: TMDBMovieService,
    private val tmdbtvShowService: TMDBTVShowService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun awaitMovie(movieId: Long): Result<List<SCredit>> = withContext(ioDispatcher) {
        suspendRunCatching {
            tmdbMovieService.credits(movieId).await()
                .body()!!
                .toSCredits()

        }
    }

    suspend fun awaitShow(showId: Long): Result<List<SCredit>> = withContext(ioDispatcher) {
        suspendRunCatching {
            tmdbtvShowService.credits(showId).await()
                .body()!!
                .toSCredits()
        }
    }
}

