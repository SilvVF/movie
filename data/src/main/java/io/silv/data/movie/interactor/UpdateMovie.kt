package io.silv.data.movie.interactor

import io.silv.data.movie.model.MovieUpdate
import io.silv.data.movie.repository.MovieRepository

class UpdateMovie(
    private val movieRepository: MovieRepository
) {

    suspend fun await(movieUpdate: MovieUpdate): Boolean {
        return movieRepository.updateMovie(movieUpdate)
    }
}
