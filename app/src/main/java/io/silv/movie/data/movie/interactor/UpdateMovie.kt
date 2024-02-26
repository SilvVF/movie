package io.silv.movie.data.movie.interactor

import io.silv.movie.data.movie.model.MovieUpdate
import io.silv.movie.data.movie.repository.MovieRepository

class UpdateMovie(
    private val movieRepository: MovieRepository
) {

    suspend fun await(movieUpdate: MovieUpdate): Boolean {
        return movieRepository.updateMovie(movieUpdate)
    }
}
