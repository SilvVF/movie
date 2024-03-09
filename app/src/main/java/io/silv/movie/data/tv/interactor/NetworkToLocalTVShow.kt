package io.silv.movie.data.tv.interactor

import io.silv.movie.data.tv.model.TVShow
import io.silv.movie.data.tv.repository.ShowRepository
import io.silv.movie.data.tv.model.toShowUpdate
import kotlinx.datetime.Clock

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
                    title = show.title,
                    posterUrl = show.posterUrl ?: localShow.posterUrl
                )
            else -> {
                val updated =  localShow.copy(
                    title = show.title,
                    posterUrl = show.posterUrl ?: localShow.posterUrl,
                    status = show.status ?: localShow.status,
                    popularity = show.popularity,
                    voteCount = show.voteCount,
                    posterLastUpdated = Clock.System.now().epochSeconds
                )
                if(showRepository.updateShow(updated.toShowUpdate())) {
                    updated
                } else {
                    localShow
                }
            }
        }
    }

    private suspend fun getTVShow(id: Long): TVShow? {
        return showRepository.getShowById(id)
    }

    private suspend fun insertShow(show: TVShow): Long? {
        return showRepository.insertShow(show)
    }
}