package io.silv.data.movie.interactor

import io.silv.core.await
import io.silv.core_network.TMDBMovieService

class GetMovieDetails(
    private val tmdbMovieService: TMDBMovieService,
) {

    suspend fun await(id: Long): String {
        return tmdbMovieService.details(id).await().body().toString()
    }
}