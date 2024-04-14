package io.silv.movie.data.lists.interactor

import io.github.jan.supabase.gotrue.Auth
import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.TVShowCoverCache
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.user.ListRepository
import kotlinx.coroutines.supervisorScope

class DeleteContentList(
    private val network: ListRepository,
    private val local: ContentListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val auth: Auth,
) {

    suspend fun await(
        list: ContentList,
        movieCoverCache: MovieCoverCache,
        showCoverCache: TVShowCoverCache,
    ): Result<Unit> {
        return runCatching {
            if (list.supabaseId != null && auth.currentUserOrNull()?.id == list.createdBy) {
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
