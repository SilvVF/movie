package io.silv.data.tv

import kotlinx.coroutines.flow.Flow

class GetShow(
    private val showRepository: ShowRepository
) {

    suspend fun await(id: Long): TVShow? {
        return showRepository.getShowById(id)
    }

    fun subscribeOrNull(id: Long): Flow<TVShow?> {
        return showRepository.observeShowByIdOrNull(id)
    }

    fun subscribe(id: Long): Flow<TVShow> {
        return showRepository.observeShowById(id)
    }
}