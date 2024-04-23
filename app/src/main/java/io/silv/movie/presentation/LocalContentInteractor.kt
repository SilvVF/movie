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
import io.silv.movie.data.lists.interactor.DeleteContentList
import io.silv.movie.data.lists.interactor.EditContentList
import io.silv.movie.data.lists.interactor.RemoveContentItemFromList
import io.silv.movie.data.lists.interactor.ToggleContentItemFavorite
import io.silv.movie.data.user.ListRepository
import io.silv.movie.data.user.ListUpdater
import io.silv.movie.presentation.ContentEvent.AddToAnotherList
import io.silv.movie.presentation.ContentEvent.AddToList
import io.silv.movie.presentation.ContentEvent.Favorite
import io.silv.movie.presentation.ContentEvent.RemoveFromList
import io.silv.movie.presentation.ListEvent.Copied
import io.silv.movie.presentation.ListEvent.Delete
import io.silv.movie.presentation.ListEvent.Edited
import io.silv.movie.presentation.ListEvent.Subscribe
import io.silv.movie.presentation.ListEvent.Unsubscribe
import io.silv.movie.presentation.ListEvent.VisibleChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val LocalContentInteractor = staticCompositionLocalOf<ContentInteractor> { error("ContentInteractor not provided in current scope") }
val LocalListInteractor = staticCompositionLocalOf<ListInteractor> { error("ListInteractor not provided in current scope") }

sealed interface ListEvent {
    data class VisibleChanged(val list: ContentList, val success: Boolean): ListEvent
    data class Copied(val list: ContentList, val success: Boolean): ListEvent
    data class Delete(val list: ContentList, val success: Boolean): ListEvent
    data class Edited(val new: ContentList, val original: ContentList, val success: Boolean): ListEvent
    data class Subscribe(val list: ContentList, val success: Boolean): ListEvent
    data class Unsubscribe(val list: ContentList, val success: Boolean): ListEvent
}

sealed interface ContentEvent {
    data class Favorite(val item: ContentItem, val success: Boolean): ContentEvent
    data class AddToList(val item: ContentItem, val list: ContentList, val success: Boolean): ContentEvent
    data class AddToAnotherList(val item: ContentItem, val list: ContentList, val success: Boolean): ContentEvent
    data class RemoveFromList(val item: ContentItem, val list: ContentList, val success: Boolean): ContentEvent
}


interface ListInteractor: EventProducer<ListEvent> {
    fun deleteList(contentList: ContentList)
    fun toggleListVisibility(contentList: ContentList)
    fun copyList(contentList: ContentList)
    fun editList(contentList: ContentList, update: (ContentList) -> ContentList)
    fun subscribeToList(contentList: ContentList)
    fun unsubscribeFromList(contentList: ContentList)
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

@Stable
class DefaultListInteractor(
    private val local: ContentListRepository,
    private val network: ListRepository,
    private val updater: ListUpdater,
    private val addContentItemToList: AddContentItemToList,
    private val editContentList: EditContentList,
    private val deleteContentList: DeleteContentList,
    private val movieCoverCache: MovieCoverCache,
    private val showCoverCache: TVShowCoverCache,
    val auth: Auth,
    private val scope: CoroutineScope,
): ListInteractor, EventProducer<ListEvent> by EventProducer.default() {

    override fun deleteList(contentList: ContentList) {
        scope.launch(Dispatchers.IO) {
            deleteContentList.await(contentList, movieCoverCache, showCoverCache)
                .onSuccess { emitEvent(Delete(contentList, true)) }
                .onFailure { emitEvent(Delete(contentList, false)) }
        }
    }

    override fun toggleListVisibility(contentList: ContentList) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                editContentList.await(contentList) {
                    it.copy(public = !it.public)
                }
                    .getOrThrow()
            }
                .onSuccess {
                    emitEvent(VisibleChanged(it, true))
                }
                .onFailure {
                    emitEvent(VisibleChanged(contentList, false))
                }
        }
    }

    override fun copyList(contentList: ContentList) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                var list = local.getList(contentList.id)

                if (list == null) {
                    updater.await(contentList.supabaseId!!)
                    list = local.getListForSupabaseId(contentList.supabaseId)!!
                }

                list = list.copy(name = list.name + "(copy)")

                val uid = auth.currentUserOrNull()?.id

                val networkList = if (uid != null) {
                    network.insertList(list.name)!!
                } else {
                    null
                }

                val id = local.createList(
                    list.name,
                    networkList?.listId,
                    uid,
                    networkList?.createdAt?.toEpochMilliseconds(),
                    networkList?.subscribers,
                    true
                )

                val copied =     local.getList(id)!!

                for (item in local.getListItems(list.id)) {
                    addContentItemToList.await(item, copied)
                }

                copied
            }
                .onSuccess { emitEvent(Copied(it, true)) }
                .onFailure { emitEvent(Copied(contentList, false)) }
        }
    }

    override fun editList(contentList: ContentList, update: (ContentList) -> ContentList) {
        scope.launch(Dispatchers.IO) {
            editContentList.await(contentList, update)
                .onSuccess { Edited(contentList, it, true) }
                .onFailure { Edited(contentList, update(contentList), false) }
        }
    }

    override fun subscribeToList(contentList: ContentList) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                var list = local.getList(contentList.id)

                if (list == null) {
                    updater.await(contentList.supabaseId!!)
                    list = local.getListForSupabaseId(contentList.supabaseId)!!
                }

                if (list.createdBy!! != auth.currentUserOrNull()!!.id) {
                    network.subscribeToList(list.supabaseId!!)
                }

                editContentList.await(list) {
                    it.copy(inLibrary = true)
                }

                list
            }
                .onSuccess { emitEvent(Subscribe(it, true)) }
                .onFailure { emitEvent(Subscribe(contentList, false)) }
        }
    }

    override fun unsubscribeFromList(contentList: ContentList) {
        scope.launch(Dispatchers.IO) {
            runCatching {

                if (contentList.createdBy ==  null || contentList.createdBy == auth.currentUserOrNull()?.id) {
                    error("cant unsubscribe from owned list")
                }

                network.unsubscribeFromList(contentList.supabaseId!!)

                editContentList.await(contentList) {
                    it.copy(inLibrary = false)
                }

                contentList
            }
                .onSuccess { emitEvent(Unsubscribe(it, true)) }
                .onFailure { emitEvent(Unsubscribe(contentList, false)) }
        }
    }
}
