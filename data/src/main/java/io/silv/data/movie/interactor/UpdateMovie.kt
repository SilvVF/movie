package io.silv.data.movie.interactor

import io.silv.data.movie.model.MovieUpdate
import io.silv.data.movie.model.ShowUpdate
import io.silv.data.movie.repository.MovieRepository
import io.silv.data.movie.repository.ShowRepository

class UpdateMovie(
    private val movieRepository: MovieRepository
) {

    suspend fun await(movieUpdate: MovieUpdate): Boolean {
        return movieRepository.updateMovie(movieUpdate)
    }
}

class UpdateShow(
    private val showRepository: ShowRepository
) {

    suspend fun await(showUpdate: ShowUpdate): Boolean {
        return showRepository.updateShow(showUpdate)
    }
}