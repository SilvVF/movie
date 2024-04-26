package io.silv.movie.data.tv.interactor

import io.silv.movie.core.STVShow
import io.silv.movie.data.cache.TVShowCoverCache
import io.silv.movie.data.tv.model.TVShow
import io.silv.movie.data.tv.model.TVShowUpdate
import io.silv.movie.data.tv.repository.ShowRepository
import kotlinx.datetime.Clock


class UpdateShow(
    private val showRepository: ShowRepository
) {

    suspend fun await(showUpdate: TVShowUpdate): Boolean {
        return showRepository.updateShow(showUpdate)
    }

    suspend fun awaitUpdateCoverLastModified(id: Long): Boolean {
        return showRepository.updateShow(
            TVShowUpdate(
                showId = id,
                posterLastUpdated = Clock.System.now().epochSeconds
            )
        )
    }

    suspend fun awaitUpdateFromSource(
        local: TVShow,
        network: STVShow,
        cache: TVShowCoverCache,
        manualFetch: Boolean = false
    ): Boolean {
        val remoteTitle = try {
            network.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // if the movie isn't a favorite, set its title from source and update in db
        val title = if (remoteTitle.isEmpty() || local.favorite) null else remoteTitle

        val coverLastModified =
            when {
                // Never refresh covers if the url is empty to avoid "losing" existing covers
                network.posterPath.isNullOrEmpty() -> null
                !manualFetch && local.posterUrl == network.posterPath -> null
                cache.getCustomCoverFile(local.id).exists() -> {
                    cache.deleteFromCache(local, false)
                    null
                }
                else -> {
                    cache.deleteFromCache(local, false)
                    Clock.System.now().toEpochMilliseconds()
                }
            }

        val thumbnailUrl = network.posterPath?.takeIf { it.isNotEmpty() }

        return showRepository.updateShow(
            TVShowUpdate(
                showId = local.id,
                title = title,
                posterLastUpdated = coverLastModified,
                productionCompanies = network.productionCompanies,
                overview = network.overview,
                genres = network.genres?.map { it.second },
                posterUrl = thumbnailUrl,
                status = network.status,
                externalUrl = network.url,
                genreIds = network.genreIds,
                originalLanguage = network.originalLanguage,
                popularity = network.popularity,
                voteCount = network.voteCount,
                releaseDate = network.releaseDate,
                favorite = null
            ),
        )
    }
}