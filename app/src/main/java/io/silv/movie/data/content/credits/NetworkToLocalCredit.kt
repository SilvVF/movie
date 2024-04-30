package io.silv.movie.data.content.credits

class NetworkToLocalCredit(
    private val creditRepository: CreditRepository,
) {
    suspend fun await(credit: io.silv.movie.data.content.credits.Credit, contentId: Long, isMovie: Boolean): io.silv.movie.data.content.credits.Credit {
        return when (val localCredit = getCredit(credit.creditId)) {
            null -> {
                insertCredit(credit, contentId, isMovie)
                credit
            }
            else -> {
                val new = localCredit.copy(
                    creditId = localCredit.creditId,
                    title = localCredit.title.ifBlank { credit.title },
                    name = localCredit.name.ifBlank { credit.name },
                    adult = localCredit.adult,
                    gender = credit.gender.takeIf { it != -1L } ?: localCredit.gender,
                    knownForDepartment = localCredit.knownForDepartment.ifBlank { credit.knownForDepartment },
                    originalName = localCredit.originalName.ifBlank { credit.originalName },
                    popularity = credit.popularity.takeIf { it!= -1.0 } ?: localCredit.popularity,
                    profilePath =  localCredit.profilePath.orEmpty().ifBlank { credit.profilePath },
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

    private suspend fun getCredit(id: String): io.silv.movie.data.content.credits.Credit? {
        return creditRepository.getById(id)
    }

    private suspend fun insertCredit(credit: io.silv.movie.data.content.credits.Credit, contentId: Long, isMovie: Boolean) {
        return creditRepository.insertCredit(credit, contentId, isMovie)
    }
}