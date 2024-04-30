package io.silv.movie.data.content.tv.interactor

import io.silv.movie.core.STVShow
import io.silv.movie.data.content.ContentPagedType
import io.silv.movie.data.content.tv.repository.SourceTVRepository
import io.silv.movie.data.content.tv.repository.TVPagingSourceType

class GetRemoteTVShows(
    private val tvRepository: SourceTVRepository
) {

    suspend fun awaitOne(showId: Long): STVShow? {
        return tvRepository.getShow(showId)
    }

    fun subscribe(type: ContentPagedType): TVPagingSourceType {
        return when (type) {
            is ContentPagedType.Search -> tvRepository.search(type.query)
            is ContentPagedType.Default -> {
                when(type) {
                    ContentPagedType.Default.Popular -> tvRepository.popular()
                    ContentPagedType.Default.TopRated -> tvRepository.topRated()
                    ContentPagedType.Default.Upcoming -> tvRepository.upcoming()
                }
            }
            is ContentPagedType.Discover -> tvRepository.discover(type.filters)
        }
    }
}