package io.silv.movie.presentation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.jan.supabase.gotrue.Auth
import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.TVShowCoverCache
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.interactor.AddContentItemToList
import io.silv.movie.data.lists.interactor.RemoveContentItemFromList
import io.silv.movie.data.lists.interactor.ToggleContentItemFavorite
import io.silv.movie.presentation.ContentEvent.AddToAnotherList
import io.silv.movie.presentation.ContentEvent.AddToList
import io.silv.movie.presentation.ContentEvent.Favorite
import io.silv.movie.presentation.ContentEvent.RemoveFromList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val LocalContentInteractor = staticCompositionLocalOf<ContentInteractor> { error("ContentInteractor not provided in current scope") }

sealed interface ContentEvent {
    data class Favorite(val item: ContentItem, val success: Boolean): ContentEvent
    data class AddToList(val item: ContentItem, val list: ContentList, val success: Boolean): ContentEvent
    data class AddToAnotherList(val item: ContentItem, val list: ContentList, val success: Boolean): ContentEvent
    data class RemoveFromList(val item: ContentItem, val list: ContentList, val success: Boolean): ContentEvent
}

interface ContentInteractor: EventProducer<ContentEvent> {
    fun toggleFavorite(contentItem: ContentItem)
    fun addToList(contentList: ContentList, contentItem: ContentItem)
    fun addToAnotherList(listId: Long, contentItem: ContentItem)
    fun addToList(listId: Long, contentItem: ContentItem)
    fun removeFromList(contentList: ContentList, contentItem: ContentItem)
}

@Stable
class DefaultContentInteractor(
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


