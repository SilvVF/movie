package io.silv.movie.presentation.library.view.list

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.toUpdate
import io.silv.movie.data.prefrences.LibraryPreferences
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.presentation.EventProducer
import io.silv.movie.presentation.asState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListViewScreenModel(
    private val contentListRepository: ContentListRepository,
    libraryPreferences: LibraryPreferences,
    listId: Long
): StateScreenModel<ListViewState>(ListViewState.Loading),
    EventProducer<ListViewEvent> by EventProducer.default() {

    private val stateSuccessTrigger =
        state.map { it.success?.list?.id }.filterNotNull().distinctUntilChanged()

    private val listSortMode = libraryPreferences.sortModeList()

    var query by mutableStateOf("")
        private set

    var listViewDisplayMode by libraryPreferences.listViewDisplayMode().asState(screenModelScope)
        private set

    init {
        screenModelScope.launch {
            val list = runCatching { contentListRepository.getList(listId) }.getOrNull()
            if (list != null) {
                mutableState.value = ListViewState.Success(
                    list = list,
                    allItems = contentListRepository.getListItems(listId).toImmutableList(),
                    sortMode = listSortMode.get()
                )
            } else {
                mutableState.value = ListViewState.Error("No list found")
            }
        }

        contentListRepository.observeListById(listId)
            .combine(stateSuccessTrigger) { a, b ->  a  }
            .onEach { list ->
                if (list != null) {
                    mutableState.updateSuccess { state ->
                        state.copy(list = list)
                    }
                } else {
                    mutableState.value = ListViewState.Error("No list found")
                }
            }
            .launchIn(screenModelScope)

        listSortMode.changes()
            .onEach { mode ->
                mutableState.updateSuccess {state -> state.copy(sortMode = mode) }
            }
            .launchIn(screenModelScope)

        state.map { it.success?.sortMode }
            .filterNotNull()
            .combine(
                snapshotFlow { query }
            ) { a, b ->  a to b }
            .flatMapLatest {  (sortMode, query) ->
                contentListRepository.observeListItemsByListId(listId, query, sortMode)
                    .onEach { content ->

                        val items = content.toImmutableList()

                        mutableState.updateSuccess { state ->
                            state.copy(items = items)
                        }
                    }
            }
            .launchIn(screenModelScope)
    }

    fun updateQuery(q: String) {
        query = q
    }

    fun updateListViewDisplayMode(mode: PosterDisplayMode) {
        listViewDisplayMode = mode
    }

    fun editList(prev: ContentList, name: String) {
        screenModelScope.launch {
            contentListRepository.updateList(
                prev.copy(name = name).toUpdate()
            )
        }
    }

    fun updateSortMode(sortMode: ListSortMode) {
        screenModelScope.launch {
            listSortMode.set(sortMode)
        }
    }


    private fun MutableStateFlow<ListViewState>.updateSuccess(
        function: (ListViewState.Success) -> ListViewState.Success
    ) {
        update {
            when (it) {
                is ListViewState.Success -> function(it)
                else -> it
            }
        }
    }

    fun deleteList() {
        screenModelScope.launch {
            val list = state.value.success?.list ?: return@launch
            runCatching { contentListRepository.deleteList(list) }
                .onSuccess {
                    emitEvent(ListViewEvent.ListDeleted)
                }
        }
    }

    fun changeDialog(dialog: Dialog?) {
        screenModelScope.launch {
            mutableState.updateSuccess { state -> state.copy(dialog = dialog) }
        }
    }

    @Stable
    sealed interface Dialog {

        @Stable
        data object DeleteList : Dialog

        @Stable
        data object ListOptions : Dialog

        @Stable
        data class ContentOptions(val item: ContentItem): Dialog
    }
}

sealed interface ListViewEvent {
    data object ListDeleted: ListViewEvent
}

sealed interface ListSortMode {
    data object Title: ListSortMode
    data object RecentlyAdded: ListSortMode
    data object Movie: ListSortMode
    data object Show: ListSortMode
}

sealed interface ListViewState {

    data object Loading: ListViewState

    data class Error(val message: String): ListViewState

    data class Success(
        val list: ContentList,
        val allItems: ImmutableList<ContentItem>,
        val sortMode: ListSortMode,
        val dialog: ListViewScreenModel.Dialog? = null,
        val items: ImmutableList<ContentItem> = persistentListOf()
    ): ListViewState

    val success: Success?
        get() = this as? Success
}