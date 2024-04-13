package io.silv.movie.data.lists.interactor

import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.TVShowCoverCache
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.toContentItem
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.GetRemoteMovie
import io.silv.movie.data.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.movie.interactor.UpdateMovie
import io.silv.movie.data.movie.model.toDomain
import io.silv.movie.data.movie.model.toMovieUpdate
import io.silv.movie.data.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.tv.interactor.UpdateShow
import io.silv.movie.data.tv.model.toDomain
import io.silv.movie.data.tv.model.toShowUpdate
import io.silv.movie.data.user.ListRepository

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
                        network.deleteMovieFromFavorites(new.id)
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