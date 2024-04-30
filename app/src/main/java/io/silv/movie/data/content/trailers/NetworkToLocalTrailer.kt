package io.silv.movie.data.content.trailers

class NetworkToLocalTrailer(
    private val trailerRepository: TrailerRepository,
) {

    suspend fun await(trailer: Trailer, contentId: Long, isMovie: Boolean): Trailer {
        return when (val localTrailer = getTrailer(trailer.id)) {
            null -> {
                insertTrailer(trailer, contentId, isMovie)
                trailer
            }
            else -> localTrailer
        }
    }

    private suspend fun getTrailer(id: String): Trailer? {
        return trailerRepository.getById(id)
    }

    private suspend fun insertTrailer(trailer: Trailer, contentId: Long, isMovie: Boolean) {
        return trailerRepository.insertTrailer(trailer, contentId, isMovie)
    }
}