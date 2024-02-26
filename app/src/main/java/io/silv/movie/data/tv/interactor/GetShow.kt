package io.silv.movie.data.tv.interactor

import io.silv.movie.data.tv.repository.ShowRepository
import io.silv.movie.data.tv.TVShow
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