package io.silv.movie.data.content.tv.interactor

import io.silv.movie.data.content.tv.model.TVShow
import io.silv.movie.data.content.tv.repository.ShowRepository

class NetworkToLocalTVShow(
    private val showRepository: ShowRepository,
) {

    suspend fun await(show: TVShow): TVShow {
        val localShow = getTVShow(show.id)
        return when {
            localShow == null -> {
                val id = insertShow(show)
                show.copy(id = id!!)
            }
            !localShow.favorite ->
                localShow.copy(
                    title = show.title
                )
            else -> localShow
        }
    }

    private suspend fun getTVShow(id: Long): TVShow? {
        return showRepository.getShowById(id)
    }

    private suspend fun insertShow(show: TVShow): Long? {
        return showRepository.insertShow(show)
    }
}