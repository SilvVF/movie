package io.silv.movie.data.content.movie.network

import io.silv.movie.core.SCredit
import io.silv.movie.core.await
import io.silv.movie.data.content.movie.local.CreditRepository
import io.silv.movie.data.content.movie.model.Credit
import io.silv.movie.data.content.movie.model.toCreditUpdate
import io.silv.movie.network.model.toSCredits
import io.silv.movie.network.service.tmdb.TMDBMovieService
import io.silv.movie.network.service.tmdb.TMDBTVShowService
import timber.log.Timber

class SourceCreditsRepository(
    private val tmdbMovieService: TMDBMovieService,
    private val tmdbtvShowService: TMDBTVShowService
) {

    suspend fun awaitMovie(movieId: Long): List<SCredit> {
        return runCatching {
            tmdbMovieService.credits(movieId).await()
                .body()!!
                .toSCredits()

        }
            .onFailure { Timber.e(it) }
            .getOrDefault(emptyList())
    }

    suspend fun awaitShow(showId: Long): List<SCredit> {
        return runCatching {
            tmdbtvShowService.credits(showId).await()
                .body()!!
                .toSCredits()
        }
            .onFailure { Timber.e(it) }
            .getOrDefault(emptyList())
    }
}

