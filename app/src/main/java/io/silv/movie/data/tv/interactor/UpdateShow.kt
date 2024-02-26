package io.silv.movie.data.tv.interactor

import io.silv.movie.data.tv.repository.ShowRepository
import io.silv.movie.data.tv.TVShowUpdate


class UpdateShow(
    private val showRepository: ShowRepository
) {

    suspend fun await(showUpdate: TVShowUpdate): Boolean {
        return showRepository.updateShow(showUpdate)
    }
}