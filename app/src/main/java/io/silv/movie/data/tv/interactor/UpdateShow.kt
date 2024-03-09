package io.silv.movie.data.tv.interactor

import io.silv.movie.data.tv.model.TVShowUpdate
import io.silv.movie.data.tv.repository.ShowRepository


class UpdateShow(
    private val showRepository: ShowRepository
) {

    suspend fun await(showUpdate: TVShowUpdate): Boolean {
        return showRepository.updateShow(showUpdate)
    }
}