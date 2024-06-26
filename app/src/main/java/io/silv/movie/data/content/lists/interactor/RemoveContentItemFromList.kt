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
import okio.IOException

class RemoveContentItemFromList(
    private val network: ListRepository,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val local: ContentListRepository,
    private val auth: Auth,
){
    suspend fun await(
        contentItem: ContentItem,
        list: ContentList,
        movieCoverCache: MovieCoverCache,
        showCoverCache: TVShowCoverCache,
    ): Result<Unit> {

        if (auth.currentUserOrNull()?.id != list.createdBy && list.createdBy != null) {
            return Result.failure(IOException("Unable to edit unowned list"))
        }

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