package io.silv.movie.data.credits

interface CreditRepository {
    suspend fun insertCredit(credit: Credit, contentId: Long, isMovie: Boolean)
    suspend fun updateCredit(update: CreditUpdate): Boolean
    suspend fun getById(id: Long): Credit?
    suspend fun getByMovieId(movieId: Long): List<Credit>
    suspend fun getByShowId(showId: Long): List<Credit>
}

class CreditRepositoryImpl(
    private val handler: io.silv.movie.database.DatabaseHandler
): CreditRepository {

    override suspend fun insertCredit(credit: Credit, contentId: Long, isMovie: Boolean) {
        handler.await(inTransaction = true) {
            creditsQueries.insert(
                id = credit.id,
                adult = credit.adult,
                gender = credit.gender,
                knownForDepartment = credit.knownForDepartment,
                name = credit.name,
                originalName = credit.originalName,
                popularity = credit.popularity,
                profilePath = credit.profilePath,
                character = credit.character,
                creditId = credit.creditId,
                crew = credit.crew,
                ordering = credit.order
            )
            if (isMovie) {
                creditsQueries.insertMovieCredit(contentId, credit.id)
            } else {
                creditsQueries.insertShowCredit(contentId, credit.id)
            }
        }
    }

    override suspend fun getByMovieId(movieId: Long): List<Credit> {
        return handler.awaitList { creditsQueries.selectByMovieId(movieId, CreditsMapper.mapCredit)}
    }

    override suspend fun getByShowId(showId: Long): List<Credit> {
        return handler.awaitList { creditsQueries.selectByShowId(showId, CreditsMapper.mapCredit)}
    }


    override suspend fun updateCredit(update: CreditUpdate): Boolean {
        return runCatching {
            partialUpdateCredit(update)
        }
            .isSuccess
    }

    override suspend fun getById(id: Long): Credit? {
        return handler.awaitOneOrNull { creditsQueries.selectById(id, CreditsMapper.mapCredit) }
    }

    private suspend fun partialUpdateCredit(vararg updates: CreditUpdate) {
        return handler.await {
            updates.forEach { update ->
                creditsQueries.update(
                    adult = update.adult,
                    gender = update.gender,
                    knownForDepartment = update.knownForDepartment,
                    name = update.name,
                    originalName = update.originalName,
                    popularity = update.popularity,
                    profilePath = update.profilePath,
                    character = update.character,
                    creditId = update.creditId,
                    crew = update.crew,
                    ordering = update.order,
                    id = update.id
                )
            }
        }
    }
}