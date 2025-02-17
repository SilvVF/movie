package io.silv.movie.presentation.screenmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.ListUpdateManager
import io.silv.movie.prefrences.LibraryPreferences
import io.silv.movie.data.supabase.ListRepository
import io.silv.movie.presentation.EventProducer
import io.silv.movie.presentation.asState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import timber.log.Timber

class LibraryScreenModel(
    private val contentListRepository: ContentListRepository,
    preferences: LibraryPreferences,
    private val listRepository: ListRepository,
    private val listUpdateManager: ListUpdateManager,
):  StateScreenModel<LibraryState>(LibraryState()),
    EventProducer<LibraryEvent> by EventProducer.default() {

    var query by mutableStateOf("")
        private set

    private val queryFlow = snapshotFlow { query }

    var displayInList by preferences.libraryDisplayInList().asState(screenModelScope)
        private set

    var sortMode by preferences.librarySortMode().asState(screenModelScope)
        private set

    val listCount = contentListRepository.observeListCount()
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    init {
        queryFlow.flatMapLatest { query ->
            contentListRepository.observeLibraryItems(query)
        }
            .combine(
                snapshotFlow { sortMode }.distinctUntilChanged()
            ) { a, b -> Pair(a, b) }
            .onEach { (contentListItems, sortMode) ->
                mutableState.update { state ->
                    state.copy(
                        contentLists = contentListItems.applySorting(sortMode)
                    )
                }
            }
            .launchIn(screenModelScope)

        contentListRepository.observeFavorites(query, FavoritesSortMode.RecentlyAdded)
            .onEach { posters ->
                mutableState.update { state ->
                    state.copy(favorites = posters)
                }
            }
            .launchIn(screenModelScope)

        listUpdateManager.isUserListUpdateRunning()
            .onEach { refreshing ->
                mutableState.update {state ->
                    state.copy(refreshingLists = refreshing)
                }
            }
            .launchIn(screenModelScope)

        listUpdateManager.isFavoritesUpdateRunning()
            .onEach { refreshing ->
                mutableState.update {state ->
                    state.copy(refreshingFavorites = refreshing)
                }
            }
            .launchIn(screenModelScope)
    }

    fun refreshFavoritesList() {
        screenModelScope.launch {
            listUpdateManager.refreshFavorites()
        }
    }

    fun refreshUserLists() {
        screenModelScope.launch {
            listUpdateManager.refreshUserLists()
        }
    }

    fun updateSortMode(mode: LibrarySortMode) {
        sortMode = mode
    }

    fun updateListMode(listMode: Boolean) {
        displayInList = listMode
    }

    fun updateQuery(query: String) {
        this.query = query
    }

    fun createList(name: String, isOnline: Boolean) {
        screenModelScope.launch {

            if (isOnline) {
                val network = listRepository.insertList(name).getOrNull() ?: return@launch

                val id = contentListRepository.createList(
                    name = network.name,
                    supabaseId = network.listId,
                    userId = network.userId,
                    createdAt = network.createdAt.epochSeconds,
                    inLibrary = true
                )
                Timber.d("created list $id online")
                emitEvent(LibraryEvent.ListCreated(id))
            } else {
                val id = contentListRepository.createList(
                    name = name,
                    inLibrary = true,
                    supabaseId = null,
                    userId = null,
                    createdAt = Clock.System.now().epochSeconds,
                    subscribers = 0
                )
                Timber.d("created list $id offline")
                emitEvent(LibraryEvent.ListCreated(id))
            }
        }
    }

    private fun List<Pair<ContentList, List<ContentItem>>>.sortBy(sortMode: LibrarySortMode) =
        when (sortMode) {
            LibrarySortMode.Title ->
                sortedBy { (list, _) ->
                    list.name
                }
            LibrarySortMode.Count ->
                sortedByDescending { (_, items) ->
                    items.size
                }
            LibrarySortMode.RecentlyAdded -> {
                sortedByDescending { (list, _) ->
                    list.lastModified
                }
            }
        }

    private fun List<Pair<ContentList, List<ContentItem>>>.applySorting(
        sortMode: LibrarySortMode
    ): List<Pair<ContentList, List<ContentItem>>> {
        val pinned = filter { it.first.pinned }
        val notPinned = filterNot { it.first.pinned }
        return buildList {
            addAll(pinned.sortBy(sortMode))
            addAll(notPinned.sortBy(sortMode))
        }
    }

    fun updateDialog(dialog: Dialog?) {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(dialog = dialog)
            }
        }
    }

    @Stable
    sealed interface Dialog {

        @Stable
        data class FullCover(val contentList: ContentList) : Dialog

        @Stable
        data class ListOptions(val contentList: ContentList, val items: List<ContentItem>) :
            Dialog

        @Stable
        data class DeleteList(val contentList: ContentList) : Dialog
    }
}
sealed interface LibraryEvent {
    data class ListCreated(val id: Long): LibraryEvent
}

sealed interface LibrarySortMode {
    data object Title: LibrarySortMode
    data object RecentlyAdded: LibrarySortMode
    data object Count: LibrarySortMode
}


data class LibraryState(
    val dialog: LibraryScreenModel.Dialog? = null,
    val contentLists: List<Pair<ContentList, List<ContentItem>>> = emptyList(),
    val favorites: List<ContentItem> = emptyList(),
    val refreshingLists: Boolean = false,
    val refreshingFavorites: Boolean = false
)