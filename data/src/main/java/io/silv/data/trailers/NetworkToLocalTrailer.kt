package io.silv.data.trailers

class NetworkToLocalTrailer(
    private val trailerRepository: TrailerRepository,
) {

    suspend fun await(trailer: Trailer): Trailer {
        return when (val localTrailer = getTrailer(trailer.id)) {
            null -> {
                val id = insertTrailer(trailer)
                trailer.copy(id = id!!)
            }
            else -> localTrailer
        }
    }

    private suspend fun getTrailer(id: Long): Trailer? {
        return trailerRepository.getById(id)
    }

    private suspend fun insertTrailer(trailer: Trailer): Long? {
        return trailerRepository.insertTrailer(trailer)
    }
}