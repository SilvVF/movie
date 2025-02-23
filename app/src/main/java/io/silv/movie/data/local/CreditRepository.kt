package io.silv.movie.data.local

import app.cash.paging.PagingSource
import io.silv.movie.data.model.Credit
import io.silv.movie.data.model.CreditUpdate
import io.silv.movie.data.model.toCreditUpdate
import io.silv.movie.database.DatabaseHandler


interface CreditRepository {
    suspend fun insertCredit(credit: Credit, contentId: Long, isMovie: Boolean)
    suspend fun updateCredit(update: CreditUpdate): Boolean
    suspend fun getById(id: String): Credit?
    suspend fun getByMovieId(movieId: Long): List<Credit>
    suspend fun getByShowId(showId: Long): List<Credit>
    fun showCreditsPagingSource(showId: Long): PagingSource<Int, Credit>
    fun movieCreditsPagingSource(movieId: Long): PagingSource<Int, Credit>
    fun personCreditsPagingSource(personId: Long): PagingSource<Int, Credit>
}

suspend fun CreditRepository.networkToLocalCredit(
    credit: Credit,
    contentId: Long,
    isMovie: Boolean
): Credit = when (val localCredit = getById(credit.creditId)) {
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
            popularity = credit.popularity.takeIf { it != -1.0 } ?: localCredit.popularity,
            profilePath = credit.profilePath.orEmpty().ifBlank { localCredit.profilePath },
            character = credit.character.ifBlank { localCredit.character },
            crew = credit.crew,
            order = credit.order ?: localCredit.order,
            personId = credit.personId ?: localCredit.personId,
            posterPath = credit.posterPath ?: localCredit.posterPath
        )
        updateCredit(new.toCreditUpdate())
        new
    }
}

class CreditRepositoryImpl(
    private val handler: DatabaseHandler,
): CreditRepository {

    override suspend fun insertCredit(credit: Credit, contentId: Long, isMovie: Boolean) {
        handler.await(true) {
            creditsQueries.insert(
                id = credit.creditId,
                adult = credit.adult,
                gender = credit.gender,
                knownForDepartment = credit.knownForDepartment,
                name = credit.name,
                originalName = credit.originalName,
                popularity = credit.popularity,
                profilePath = credit.profilePath,
                character = credit.character,
                crew = credit.crew,
                ordering = credit.order,
                personId = credit.personId,
                movieId = contentId.takeIf { isMovie },
                showId = contentId.takeIf { !isMovie },
                posterPath = credit.posterPath,
                title = credit.title
            )
        }
    }

    override fun personCreditsPagingSource(personId: Long): PagingSource<Int, Credit> = handler.queryPagingSource(
        countQuery = { creditsQueries.countCreditsForPersonId(personId) },
        initialOffset = 0L,
        queryProvider = { limit, offset ->
            creditsQueries.selectByPersonId(personId, limit, offset, CreditsMapper.mapCredit)
        },
        transacter = { creditsQueries }
    )

    override fun showCreditsPagingSource(showId: Long): PagingSource<Int, Credit> = handler.queryPagingSource(
        countQuery = { creditsQueries.countCreditsForShowId(showId) },
        initialOffset = 0L,
        queryProvider = { limit, offset ->
            creditsQueries.selectByShowId(showId, limit, offset, CreditsMapper.mapCredit)
        },
        transacter = { creditsQueries }
    )


    override fun movieCreditsPagingSource(movieId: Long): PagingSource<Int, Credit> = handler.queryPagingSource(
        countQuery = { creditsQueries.countCreditsForMovieId(movieId) },
        initialOffset = 0L,
        queryProvider = { limit, offset ->
            creditsQueries.selectByMovieId(movieId, limit, offset, CreditsMapper.mapCredit)
        },
        transacter = { creditsQueries }
    )

    override suspend fun getByMovieId(movieId: Long): List<Credit> {
        return handler.awaitList { creditsQueries.selectByMovieId(movieId, Long.MAX_VALUE, 0,
            CreditsMapper.mapCredit
        )}
    }

    override suspend fun getByShowId(showId: Long): List<Credit> {
        return handler.awaitList { creditsQueries.selectByShowId(showId,Long.MAX_VALUE, 0,
            CreditsMapper.mapCredit
        ) }
    }



    override suspend fun updateCredit(update: CreditUpdate): Boolean {
        return runCatching {
            partialUpdateCredit(update)
        }
            .isSuccess
    }

    override suspend fun getById(id: String): Credit? {
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
                    character = update.character,
                    crew = update.crew,
                    ordering = update.order,
                    personId = update.personId,
                    id = update.creditId,
                    posterPath = update.posterPath,
                    profilePath = update.profilePath,
                    title = update.title
                )
            }
        }
    }
}