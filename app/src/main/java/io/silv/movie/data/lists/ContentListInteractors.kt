package io.silv.movie.data.lists

import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.TVShowCoverCache
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.UpdateMovie
import io.silv.movie.data.movie.model.toMovieUpdate
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.UpdateShow
import io.silv.movie.data.tv.model.toShowUpdate
import io.silv.movie.data.user.ListRepository
import io.silv.movie.data.user.toUserListUpdate
import kotlinx.coroutines.supervisorScope
import okio.IOException


class EditContentList(
    private val network: ListRepository,
    private val local: ContentListRepository
) {

    suspend fun await(
        list: ContentList,
        update: (prev: ContentList) -> ContentList
    ): Result<Unit> {
        val new = update(list)

        if (new.supabaseId != null) {
            val result = network.updateList(new.toUserListUpdate())
            if (!result) {
                return Result.failure(IOException("Failed to remove from network"))
            }
        }
        local.updateList(new.toUpdate())

        return Result.success(Unit)
    }
}

class AddContentItemToList(
    private val network: ListRepository,
    private val local: ContentListRepository,
) {

    suspend fun await(
        contentItem: ContentItem,
        list: ContentList
    ): Result<Unit> {
        if (list.supabaseId != null) {
            val result = if (contentItem.isMovie) {
                network.addMovieToList(contentItem.contentId, list)
            } else {
                network.addShowToList(contentItem.contentId, list)
            }

            if (!result) {
                return Result.failure(IOException("Failed to remove from network"))
            }
        }

        if (contentItem.isMovie) {
            local.addMovieToList(contentItem.contentId, list)
        } else {
            local.addShowToList(contentItem.contentId, list)
        }

        return Result.success(Unit)
    }
}

class RemoveContentItemFromList(
    private val network: ListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val local: ContentListRepository,
){
    suspend fun await(
        contentItem: ContentItem,
        list: ContentList,
        movieCoverCache: MovieCoverCache,
        showCoverCache: TVShowCoverCache,
    ): Result<Unit> {
        if (list.supabaseId != null) {
            val result = if (contentItem.isMovie) {
                network.deleteMovieFromList(contentItem.contentId, list)
            } else {
                network.deleteShowFromList(contentItem.contentId, list)
            }

            if (!result) {
                return Result.failure(IOException("Failed to remove from network"))
            }
        }
        deleteContentItemFromCache(contentItem, movieCoverCache, showCoverCache)
        if (contentItem.isMovie) {
            local.removeMovieFromList(contentItem.contentId, list)
        } else {
            local.removeShowFromList(contentItem.contentId, list)
        }

        return Result.success(Unit)
    }

    private suspend fun deleteContentItemFromCache(
        item: ContentItem,
        movieCoverCache: MovieCoverCache,
        showCoverCache: TVShowCoverCache
    ) {
        if (item.inLibraryLists == 1L && !item.favorite) {
            if (item.isMovie) {
                val movie = getMovie.await(item.contentId) ?: return
                movieCoverCache.deleteFromCache(movie)
            } else {
                val show = getShow.await(item.contentId) ?: return
                showCoverCache.deleteFromCache(show)
            }
        }
    }
}

class DeleteContentList(
    private val network: ListRepository,
    private val local: ContentListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
) {

    suspend fun await(
        list: ContentList,
        movieCoverCache: MovieCoverCache,
        showCoverCache: TVShowCoverCache,
    ): Result<Unit> {
        return runCatching {
            if (list.supabaseId != null) {
                val result = network.deleteList(list.supabaseId)

                if (!result) {
                    error("Failed to remove list from network")
                }
            }
            supervisorScope {
                if (list.inLibrary) {
                    for (item in  local.getListItems(list.id)) {
                        deleteContentItemFromCache(item, movieCoverCache, showCoverCache)
                    }
                }
            }
            local.deleteList(list)
        }
    }

    private suspend fun deleteContentItemFromCache(
        item: ContentItem,
        movieCoverCache: MovieCoverCache,
        showCoverCache: TVShowCoverCache
    ) {
        if (item.inLibraryLists == 1L && !item.favorite) {
            if (item.isMovie) {
                val movie = getMovie.await(item.contentId) ?: return
                movieCoverCache.deleteFromCache(movie)
            } else {
                val show = getShow.await(item.contentId) ?: return
                showCoverCache.deleteFromCache(show)
            }
        }
    }
}


class ToggleContentItemFavorite(
    private val network: ListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val updateMovie: UpdateMovie,
    private val updateShow: UpdateShow,
    private val movieCoverCache: MovieCoverCache,
    private val showCoverCache: TVShowCoverCache,
) {
    suspend fun await(
        contentItem: ContentItem,
        changeOnNetwork: Boolean = false
    ) {
        if (contentItem.isMovie) {
            val movie = getMovie.await(contentItem.contentId) ?: return
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
        } else {
            val show = getShow.await(contentItem.contentId) ?: return
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
        }
    }
}