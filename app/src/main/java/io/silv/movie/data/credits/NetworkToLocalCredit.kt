package io.silv.movie.data.credits

class NetworkToLocalCredit(
    private val creditRepository: CreditRepository,
) {

    suspend fun await(credit: Credit, contentId: Long, isMovie: Boolean): Credit {
        return when (val localTrailer = getCredit(credit.id)) {
            null -> {
                insertCredit(credit, contentId, isMovie)
                credit
            }
            else -> localTrailer
        }
    }

    private suspend fun getCredit(id: Long): Credit? {
        return creditRepository.getById(id)
    }

    private suspend fun insertCredit(credit: Credit, contentId: Long, isMovie: Boolean) {
        return creditRepository.insertCredit(credit, contentId, isMovie)
    }
}