package io.silv.movie.data.content.credits

import io.silv.movie.core.SCredit
import io.silv.movie.core.await
import io.silv.movie.network.model.toSCredits
import io.silv.movie.network.service.tmdb.TMDBMovieService
import io.silv.movie.network.service.tmdb.TMDBTVShowService
import timber.log.Timber

class GetRemoteCredits(
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

class GetTVShowCredits(
    private val creditRepository: CreditRepository
) {
    suspend fun await(showId: Long): List<io.silv.movie.data.content.credits.Credit> {
        return creditRepository.getByShowId(showId)
    }
}

class GetMovieCredits(
    private val creditRepository: CreditRepository
) {

    suspend fun await(movieId: Long): List<io.silv.movie.data.content.credits.Credit> {
        return creditRepository.getByMovieId(movieId)
    }
}