package io.silv.movie.data.content.lists.interactor

import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.toContentItem
import io.silv.movie.data.content.movie.interactor.GetMovie
import io.silv.movie.data.content.movie.interactor.GetRemoteMovie
import io.silv.movie.data.content.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.content.movie.interactor.UpdateMovie
import io.silv.movie.data.content.movie.model.toDomain
import io.silv.movie.data.content.movie.model.toMovieUpdate
import io.silv.movie.data.content.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.content.tv.interactor.GetShow
import io.silv.movie.data.content.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.content.tv.interactor.UpdateShow
import io.silv.movie.data.content.tv.model.toDomain
import io.silv.movie.data.content.tv.model.toShowUpdate
import io.silv.movie.data.user.repository.ListRepository
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache

class ToggleContentItemFavorite(
    private val network: ListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val updateMovie: UpdateMovie,
    private val updateShow: UpdateShow,
    private val movieCoverCache: MovieCoverCache,
    private val showCoverCache: TVShowCoverCache,
    private val networkToLocalTVShow: NetworkToLocalTVShow,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val getRemoteTVShows: GetRemoteTVShows,
    private val getRemoteMovie: GetRemoteMovie,
) {
    suspend fun await(
        contentItem: ContentItem,
        changeOnNetwork: Boolean = false
    ): Result<ContentItem> {
        return runCatching {
            if (contentItem.isMovie) {
                val movie = getMovie.await(contentItem.contentId) ?: run {
                    networkToLocalMovie.await(getRemoteMovie.awaitOne(contentItem.contentId)!!.toDomain())
                }
                val new = movie.copy(favorite = !movie.favorite)

                if (changeOnNetwork) {
                    if (new.favorite) {
                        network.addMovieToFavoritesList(new)
                    } else {
                        network.deleteMovieFromFavorites(new.id)
                    }
                }

                if(!new.favorite && !new.inList) {
                    movieCoverCache.deleteFromCache(movie)
                }
                updateMovie.await(new.toMovieUpdate())
                new.toContentItem()
            } else {
                val show = getShow.await(contentItem.contentId) ?: run {
                    networkToLocalTVShow.await(getRemoteTVShows.awaitOne(contentItem.contentId)!!.toDomain())
                }
                val new = show.copy(favorite = !show.favorite)

                if (changeOnNetwork) {
                    if (new.favorite) {
                        network.addShowToFavorites(new)
                    } else {
                        network.deleteShowFromFavorites(new.id)
                    }
                }

                if(!new.favorite && !new.inList) {
                    showCoverCache.deleteFromCache(show)
                }
                updateShow.await(new.toShowUpdate())
                new.toContentItem()
            }
        }
    }
}