package io.silv.data

class NetworkToLocalMovie(
    private val movieRepository: MovieRepository,
) {

    suspend fun await(movie: Movie): Movie {
        val localMovie = getMovie(movie.id)
        return when {
            localMovie == null -> {
                val id = insertMovie(movie)
                movie.copy(id = id!!)
            }
            !localMovie.favorite ->
                // if the manga isn't a favorite, set its display title from source
                // if it later becomes a favorite, updated title will go to db
                localMovie.copy(title = movie.title)
            else -> localMovie
        }
    }

    private suspend fun getMovie(id: Long): Movie? {
        return movieRepository.getMovieById(id)
    }

    private suspend fun insertMovie(movie: Movie): Long? {
        return movieRepository.insertMovie(movie)
    }
}