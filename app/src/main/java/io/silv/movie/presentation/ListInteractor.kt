package io.silv.movie.presentation

import io.github.jan.supabase.auth.Auth
import io.silv.movie.IoDispatcher
import io.silv.movie.data.DeleteContentList
import io.silv.movie.data.EditContentList
import io.silv.movie.data.ListUpdateManager
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.model.toUpdate
import io.silv.movie.data.supabase.ListRepository
import io.silv.movie.presentation.ListInteractor.ListEvent
import io.silv.movie.presentation.ListInteractor.ListEvent.Copied
import io.silv.movie.presentation.ListInteractor.ListEvent.Delete
import io.silv.movie.presentation.ListInteractor.ListEvent.Edited
import io.silv.movie.presentation.ListInteractor.ListEvent.Pinned
import io.silv.movie.presentation.ListInteractor.ListEvent.Subscribe
import io.silv.movie.presentation.ListInteractor.ListEvent.Unsubscribe
import io.silv.movie.presentation.ListInteractor.ListEvent.VisibleChanged
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface ListInteractor: EventProducer<ListEvent> {
    fun deleteList(contentList: ContentList)
    fun toggleListVisibility(contentList: ContentList)
    fun copyList(contentList: ContentList)
    fun editList(contentList: ContentList, update: (ContentList) -> ContentList)
    fun subscribeToList(contentList: ContentList)
    fun unsubscribeFromList(contentList: ContentList)
    fun togglePinned(contentList: ContentList)

    sealed interface ListEvent {
        data class VisibleChanged(val list: ContentList, val success: Boolean): ListEvent
        data class Copied(val list: ContentList, val success: Boolean): ListEvent
        data class Delete(val list: ContentList, val success: Boolean): ListEvent
        data class Edited(val new: ContentList, val original: ContentList, val success: Boolean): ListEvent
        data class Subscribe(val list: ContentList, val success: Boolean): ListEvent
        data class Unsubscribe(val list: ContentList, val success: Boolean): ListEvent
        data class Pinned(val list: ContentList): ListEvent
    }
}

class DefaultListInteractor(
    private val local: ContentListRepository,
    private val network: ListRepository,
    private val listUpdateManager: ListUpdateManager,
    private val editContentList: EditContentList,
    private val deleteContentList: DeleteContentList,
    private val movieCoverCache: MovieCoverCache,
    private val showCoverCache: TVShowCoverCache,
    val auth: Auth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): ListInteractor, EventProducer<ListEvent> by EventProducer.default() {

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    
    override fun deleteList(contentList: ContentList) {
        scope.launch(ioDispatcher) {
            deleteContentList(contentList, movieCoverCache, showCoverCache)
                .onSuccess { emitEvent(Delete(contentList, true)) }
                .onFailure { emitEvent(Delete(contentList, false)) }
        }
    }

    override fun toggleListVisibility(contentList: ContentList) {
        scope.launch(ioDispatcher) {
            runCatching {
                editContentList(contentList) {
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
        scope.launch(ioDispatcher) {
            runCatching {
                var list = local.getList(contentList.id)

                if (list == null) {
                    listUpdateManager.awaitRefresh(contentList.supabaseId!!)
                    list = local.getListForSupabaseId(contentList.supabaseId)!!
                }

                list = list.copy(name = list.name + "(copy)")

                val uid = auth.currentUserOrNull()?.id

                val networkList = if (uid != null) {
                    network.insertList(list.name).getOrNull()
                } else {
                    null
                }

                val id = local.createList(
                    list.name,
                    networkList?.listId,
                    uid,
                    networkList?.createdAt?.epochSeconds,
                    networkList?.subscribers,
                    true
                )

                val copied =     local.getList(id)!!

                for (item in local.getListItems(list.id)) {
                    addContentItemToList(auth, network, local,item, copied)
                }

                copied
            }
                .onSuccess { emitEvent(Copied(it, true)) }
                .onFailure { emitEvent(Copied(contentList, false)) }
        }
    }

    override fun editList(contentList: ContentList, update: (ContentList) -> ContentList) {
        scope.launch(ioDispatcher) {
            editContentList(contentList, update)
                .onSuccess { Edited(contentList, it, true) }
                .onFailure { Edited(contentList, update(contentList), false) }
        }
    }

    override fun subscribeToList(contentList: ContentList) {
        scope.launch(ioDispatcher) {
            runCatching {
                var list = local.getList(contentList.id)

                if (list == null) {
                    listUpdateManager.awaitRefresh(contentList.supabaseId!!)
                    list = local.getListForSupabaseId(contentList.supabaseId)!!
                }

                if (list.createdBy!! != auth.currentUserOrNull()!!.id) {
                    network.subscribeToList(list.supabaseId!!)
                }

                editContentList(list) {
                    it.copy(inLibrary = true)
                }

                list
            }
                .onSuccess { emitEvent(Subscribe(it, true)) }
                .onFailure { emitEvent(Subscribe(contentList, false)) }
        }
    }

    override fun unsubscribeFromList(contentList: ContentList) {
        scope.launch(ioDispatcher) {
            runCatching {

                if (contentList.createdBy ==  null || contentList.createdBy == auth.currentUserOrNull()?.id) {
                    error("cant unsubscribe from owned list")
                }

                network.unsubscribeFromList(contentList.supabaseId!!)

                editContentList(contentList) {
                    it.copy(inLibrary = false)
                }

                contentList
            }
                .onSuccess { emitEvent(Unsubscribe(it, true)) }
                .onFailure { emitEvent(Unsubscribe(contentList, false)) }
        }
    }

    override fun togglePinned(contentList: ContentList) {
        scope.launch(ioDispatcher) {
            runCatching {
                val new = contentList.copy(pinned = !contentList.pinned)
                local.updateList(new.toUpdate())
                new
            }
                .onSuccess { emitEvent(Pinned(it)) }
                .onFailure { emitEvent(Pinned(contentList)) }
        }
    }
}