package io.silv.movie.presentation

import androidx.compose.runtime.Stable
import io.github.jan.supabase.gotrue.Auth
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.content.lists.interactor.AddContentItemToList
import io.silv.movie.data.content.lists.interactor.RemoveContentItemFromList
import io.silv.movie.data.content.lists.interactor.ToggleContentItemFavorite
import io.silv.movie.data.content.lists.repository.ContentListRepository
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


interface ContentInteractor: EventProducer<ContentEvent> {
    fun toggleFavorite(contentItem: ContentItem)
    fun addToList(contentList: ContentList, contentItem: ContentItem)
    fun addToAnotherList(listId: Long, contentItem: ContentItem)
    fun addToList(listId: Long, contentItem: ContentItem)
    fun removeFromList(contentList: ContentList, contentItem: ContentItem)

    companion object {
        fun default(
            toggleContentItemFavorite: ToggleContentItemFavorite,
            removeContentItemFromList: RemoveContentItemFromList,
            addContentItemToList: AddContentItemToList,
            contentListRepository: ContentListRepository,
            auth: Auth,
            movieCoverCache: MovieCoverCache,
            showCoverCache: TVShowCoverCache,
            scope: CoroutineScope,
        ): ContentInteractor {
            return DefaultContentInteractor(
                toggleContentItemFavorite,
                removeContentItemFromList,
                addContentItemToList,
                contentListRepository,
                auth,
                movieCoverCache,
                showCoverCache,
                scope
            )
        }
    }

    sealed interface ContentEvent {
        data class Favorite(val item: ContentItem, val success: Boolean): ContentEvent
        data class AddToList(val item: ContentItem, val list: ContentList, val success: Boolean): ContentEvent
        data class AddToAnotherList(val item: ContentItem, val list: ContentList, val success: Boolean): ContentEvent
        data class RemoveFromList(val item: ContentItem, val list: ContentList, val success: Boolean): ContentEvent
    }
}

@Stable
private class DefaultContentInteractor(
    private val toggleContentItemFavorite: ToggleContentItemFavorite,
    private val removeContentItemFromList: RemoveContentItemFromList,
    private val addContentItemToList: AddContentItemToList,
    private val contentListRepository: ContentListRepository,
    private val auth: Auth,
    private val movieCoverCache: MovieCoverCache,
    private val showCoverCache: TVShowCoverCache,
    private val scope: CoroutineScope,
): ContentInteractor, EventProducer<ContentEvent> by EventProducer.default() {

    override fun toggleFavorite(contentItem: ContentItem) {
        scope.launch(Dispatchers.IO) {
            toggleContentItemFavorite.await(
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
            addContentItemToList.await(contentItem, contentList)
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
            addContentItemToList.await(contentItem, list)
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
            removeContentItemFromList.await(contentItem, contentList, movieCoverCache, showCoverCache)
                .onSuccess {
                    emitEvent(RemoveFromList(contentItem, contentList, true))
                }
                .onFailure {
                    emitEvent(RemoveFromList(contentItem, contentList, false))
                }
        }
    }

}


