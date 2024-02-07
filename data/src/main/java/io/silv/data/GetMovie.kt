package io.silv.data

import kotlinx.coroutines.flow.Flow

class GetMovie(
    private val movieRepository: MovieRepository
) {

    suspend fun await(id: Long): Movie? {
        return movieRepository.getMovieById(id)
    }

    fun subscribe(id: Long): Flow<Movie> {
        return movieRepository.observeMovieById(id)
    }
}