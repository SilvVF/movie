package io.silv.movie.data.content.credits

class NetworkToLocalCredit(
    private val creditRepository: CreditRepository,
) {
    suspend fun await(credit: Credit, contentId: Long, isMovie: Boolean): Credit {
        return when (val localCredit = getCredit(credit.creditId)) {
            null -> {
                insertCredit(credit, contentId, isMovie)
                credit
            }
            else -> {
                val new = localCredit.copy(
                    creditId = localCredit.creditId,
                    title = credit.title.ifBlank { localCredit.title },
                    name = credit.name.ifBlank { localCredit.name },
                    adult = credit.adult,
                    gender = credit.gender.takeIf { it != -1L } ?: localCredit.gender,
                    knownForDepartment = credit.knownForDepartment.ifBlank { localCredit.knownForDepartment },
                    originalName = credit.originalName.ifBlank { localCredit.originalName },
                    popularity = credit.popularity.takeIf { it!= -1.0 } ?: localCredit.popularity,
                    profilePath =  credit.profilePath.orEmpty().ifBlank { localCredit.profilePath },
                    character = credit.character.ifBlank { localCredit.character },
                    crew = credit.crew,
                    order = credit.order ?: localCredit.order,
                    personId = credit.personId ?: localCredit.personId,
                    posterPath = credit.posterPath ?: localCredit.posterPath
                )
                creditRepository.updateCredit(new.toCreditUpdate())
                new
            }
        }
    }

    private suspend fun getCredit(id: String): Credit? {
        return creditRepository.getById(id)
    }

    private suspend fun insertCredit(credit: Credit, contentId: Long, isMovie: Boolean) {
        return creditRepository.insertCredit(credit, contentId, isMovie)
    }
}