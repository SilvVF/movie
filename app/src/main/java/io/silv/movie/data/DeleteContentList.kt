package io.silv.movie.data

import io.github.jan.supabase.auth.Auth
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.data.supabase.ListRepository
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import kotlinx.coroutines.supervisorScope



class DeleteContentList(
    private val network: ListRepository,
    private val listRepo: ContentListRepository,
    private val local: LocalContentDelegate,
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
                    for (item in  listRepo.getListItems(list.id)) {
                        deleteContentItemFromCache(item, movieCoverCache, showCoverCache)
                    }
                }
            }
            listRepo.deleteList(list)
        }
    }

    private suspend fun deleteContentItemFromCache(
        item: ContentItem,
        movieCoverCache: MovieCoverCache,
        showCoverCache: TVShowCoverCache
    ) {
        if (item.inLibraryLists == 1L && !item.favorite) {
            if (item.isMovie) {
                val movie = local.getMovieById(item.contentId) ?: return
                movieCoverCache.deleteFromCache(movie)
            } else {
                val show = local.getShowById(item.contentId) ?: return
                showCoverCache.deleteFromCache(show)
            }
        }
    }
}
