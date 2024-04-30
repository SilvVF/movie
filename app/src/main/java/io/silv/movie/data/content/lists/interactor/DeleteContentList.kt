package io.silv.movie.data.content.lists.interactor

import io.github.jan.supabase.gotrue.Auth
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.content.lists.repository.ContentListRepository
import io.silv.movie.data.content.movie.interactor.GetMovie
import io.silv.movie.data.content.tv.interactor.GetShow
import io.silv.movie.data.user.repository.ListRepository
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
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
