package io.silv.movie.presentation

import androidx.compose.runtime.Stable
import io.github.jan.supabase.auth.Auth
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.model.toContentItem
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.data.local.networkToLocalMovie
import io.silv.movie.data.local.networkToLocalShow
import io.silv.movie.data.model.toDomain
import io.silv.movie.data.model.toMovieUpdate
import io.silv.movie.data.model.toShowUpdate
import io.silv.movie.data.network.NetworkContentDelegate
import io.silv.movie.data.supabase.ContentType
import io.silv.movie.data.supabase.ListRepository
import io.silv.movie.presentation.ContentInteractor.ContentEvent
import io.silv.movie.presentation.ContentInteractor.ContentEvent.AddToAnotherList
import io.silv.movie.presentation.ContentInteractor.ContentEvent.AddToList
import io.silv.movie.presentation.ContentInteractor.ContentEvent.Favorite
import io.silv.movie.presentation.ContentInteractor.ContentEvent.RemoveFromList
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

@Stable
interface ContentInteractor : EventProducer<ContentEvent> {
    fun toggleFavorite(contentItem: ContentItem)
    fun addToList(contentList: ContentList, contentItem: ContentItem)
    fun addToAnotherList(listId: Long, contentItem: ContentItem)
    fun addToList(listId: Long, contentItem: ContentItem)
    fun removeFromList(contentList: ContentList, contentItem: ContentItem)

    companion object {
        fun default(
            local: LocalContentDelegate,
            listRepository: ListRepository,
            contentListRepository: ContentListRepository,
            network: NetworkContentDelegate,
            auth: Auth,
            movieCoverCache: MovieCoverCache,
            showCoverCache: TVShowCoverCache,
            scope: CoroutineScope,
        ): ContentInteractor {
            return DefaultContentInteractor(
                contentListRepository,
                listRepository,
                local,
                auth,
                network,
                movieCoverCache,
                showCoverCache,
                scope
            )
        }
    }

    sealed interface ContentEvent {
        data class Favorite(val item: ContentItem, val success: Boolean) : ContentEvent
        data class AddToList(val item: ContentItem, val list: ContentList, val success: Boolean) :
            ContentEvent

        data class AddToAnotherList(
            val item: ContentItem,
            val list: ContentList,
            val success: Boolean
        ) : ContentEvent

        data class RemoveFromList(
            val item: ContentItem,
            val list: ContentList,
            val success: Boolean
        ) : ContentEvent
    }
}

@Stable
private class DefaultContentInteractor(
    val contentListRepository: ContentListRepository,
    val listRepository: ListRepository,
    val local: LocalContentDelegate,
    val auth: Auth,
    val network: NetworkContentDelegate,
    val movieCoverCache: MovieCoverCache,
    val showCoverCache: TVShowCoverCache,
    val scope: CoroutineScope,
) : ContentInteractor, EventProducer<ContentEvent> by EventProducer.default() {

    override fun toggleFavorite(contentItem: ContentItem) {
        scope.launch(Dispatchers.IO) {
            toggleContentItemFavorite(
                contentItem,
                changeOnNetwork = auth.currentUserOrNull() != null
            )
                .onSuccess {
                    emitEvent(Favorite(it, true))
                }
                .onFailure {
                    emitEvent(Favorite(contentItem, false))
                }
        }
    }

    override fun addToList(contentList: ContentList, contentItem: ContentItem) {
        scope.launch(Dispatchers.IO) {
            addContentItemToList(auth, listRepository, contentListRepository, contentItem, contentList)
                .onSuccess {
                    emitEvent(AddToList(contentItem, contentList, true))
                }
                .onFailure {
                    emitEvent(AddToList(contentItem, contentList, false))
                }
        }
    }

    override fun addToAnotherList(listId: Long, contentItem: ContentItem) {
        scope.launch(Dispatchers.IO) {
            val list = contentListRepository.getList(listId) ?: return@launch
            addContentItemToList(auth, listRepository, contentListRepository, contentItem, list)
                .onSuccess {
                    emitEvent(AddToAnotherList(contentItem, list, true))
                }
                .onFailure {
                    emitEvent(AddToAnotherList(contentItem, list, false))
                }
        }
    }

    override fun addToList(listId: Long, contentItem: ContentItem) {
        scope.launch(Dispatchers.IO) {
            val list = contentListRepository.getList(listId) ?: return@launch
            addToList(list, contentItem)
        }
    }

    override fun removeFromList(contentList: ContentList, contentItem: ContentItem) {
        scope.launch(Dispatchers.IO) {
            removeContentItemFromList(
                contentItem,
                contentList,
            )
                .onSuccess {
                    emitEvent(RemoveFromList(contentItem, contentList, true))
                }
                .onFailure {
                    emitEvent(RemoveFromList(contentItem, contentList, false))
                }
        }
    }
}

private suspend fun DefaultContentInteractor.toggleContentItemFavorite(
    contentItem: ContentItem,
    changeOnNetwork: Boolean = false
): Result<ContentItem> {
    return runCatching {
        if (contentItem.isMovie) {
            val movie = local.getMovieById(contentItem.contentId) ?: run {
                local.networkToLocalMovie(
                    network.getMovie(contentItem.contentId)!!.toDomain()
                )
            }
            val new = movie.copy(favorite = !movie.favorite)

            if (changeOnNetwork) {
                if (new.favorite) {
                    listRepository.addMovieToFavoritesList(new)
                } else {
                    listRepository.deleteFromFavorites(new.id, ContentType.Movie)
                }
            }

            if (!new.favorite && !new.inList) {
                movieCoverCache.deleteFromCache(movie)
            }
            local.updateMovie(new.toMovieUpdate())
            new.toContentItem()
        } else {
            val show = local.getShowById(contentItem.contentId) ?: run {
                local.networkToLocalShow(
                    network.getShow(contentItem.contentId)!!.toDomain()
                )
            }
            val new = show.copy(favorite = !show.favorite)

            if (changeOnNetwork) {
                if (new.favorite) {
                    listRepository.addShowToFavorites(new)
                } else {
                    listRepository.deleteFromFavorites(new.id, ContentType.Show)
                }
            }

            if (!new.favorite && !new.inList) {
                showCoverCache.deleteFromCache(show)
            }
            local.updateShow(new.toShowUpdate())
            new.toContentItem()
        }
    }
}

suspend fun addContentItemToList(
    auth: Auth,
    listRepository: ListRepository,
    contentListRepository: ContentListRepository,
    contentItem: ContentItem,
    list: ContentList,
): Result<Unit> {
    if (auth.currentUserOrNull()?.id != list.createdBy && list.createdBy != null) {
        return Result.failure(IOException("Unable to edit unowned list"))
    }

    if (list.supabaseId != null) {
        val result =  listRepository.addToList(
            contentItem.contentId,
            contentItem.posterUrl,
            contentItem.title,
            list,
            contentItem.toType()
        )

        if (!result) {
            return Result.failure(IOException("Failed to remove from network"))
        }
    }

    if (contentItem.isMovie) {
        contentListRepository.addMovieToList(contentItem.contentId, list, null)
    } else {
        contentListRepository.addShowToList(contentItem.contentId, list, null)
    }

    return Result.success(Unit)
}

private suspend fun DefaultContentInteractor.removeContentItemFromList(
    item: ContentItem,
    list: ContentList,
): Result<Unit> {

    if (auth.currentUserOrNull()?.id != list.createdBy && list.createdBy != null) {
        return Result.failure(IOException("Unable to edit unowned list"))
    }

    if (list.supabaseId != null) {
        val result = listRepository.deleteFromList(item.contentId, list, item.toType())
        if (!result) {
            return Result.failure(IOException("Failed to remove from network"))
        }
    }

    if (item.inLibraryLists == 1L && !item.favorite) {
        if (item.isMovie) {
            local.getMovieById(item.contentId)?.let {
                movieCoverCache.deleteFromCache(it)
            }
        } else {
            local.getShowById(item.contentId)?.let {
                showCoverCache.deleteFromCache(it)
            }
        }
    }

    if (item.isMovie) {
        contentListRepository.removeMovieFromList(item.contentId, list)
    } else {
        contentListRepository.removeShowFromList(item.contentId, list)
    }

    return Result.success(Unit)
}


