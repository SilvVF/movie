package io.silv.movie.presentation.library.screenmodels

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListItem
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.GetFavoritesList
import io.silv.movie.data.prefrences.LibraryPreferences
import io.silv.movie.data.user.FavoritesUpdateManager
import io.silv.movie.data.user.ListRepository
import io.silv.movie.data.user.UserListUpdateManager
import io.silv.movie.presentation.EventProducer
import io.silv.movie.presentation.asState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
    getFavoritesList: GetFavoritesList,
    private val listRepository: ListRepository,
    private val userListUpdateManager: UserListUpdateManager,
    private val favoritesUpdateManager: FavoritesUpdateManager,
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
                val grouped = contentListItems
                    .groupBy { item -> item.list.id }
                    .mapKeys { (k, _) ->
                        contentListItems.first { it.list.id == k }.list
                    }
                    .mapValues { (_, items) ->
                        items
                            .filterIsInstance<ContentListItem.Item>()
                            .sortedBy { it.createdAt }
                            .map { it.contentItem }
                            .toImmutableList()
                    }
                    .applySorting(sortMode)
                    .toImmutableList()

                mutableState.update { state ->
                    state.copy(
                        contentLists = grouped
                    )
                }
            }
            .launchIn(screenModelScope)

        getFavoritesList.subscribe()
            .onEach { posters ->
                mutableState.update { state ->
                    state.copy(
                        favorites = posters.toImmutableList()
                    )
                }
            }
            .launchIn(screenModelScope)

        userListUpdateManager.isRunning()
            .onEach { refreshing ->
                mutableState.update {state ->
                    state.copy(refreshingLists = refreshing)
                }
            }
            .launchIn(screenModelScope)

        favoritesUpdateManager.isRunning()
            .onEach { refreshing ->
                mutableState.update {state ->
                    state.copy(refreshingFavorites = refreshing)
                }
            }
            .launchIn(screenModelScope)
    }

    fun refreshFavoritesList() {
        screenModelScope.launch {
            favoritesUpdateManager.refreshFavorites()
        }
    }

    fun refreshUserLists() {
        screenModelScope.launch {
            userListUpdateManager.refreshUserLists()
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
                val network = listRepository.insertList(name) ?: return@launch

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

    private fun Map<ContentList, ImmutableList<ContentItem>>.applySorting(
        sortMode: LibrarySortMode
    ): List<Pair<ContentList, ImmutableList<ContentItem>>> {
        return when (sortMode) {
            LibrarySortMode.Title ->
                toSortedMap { a: ContentList, b: ContentList ->
                    a.name.compareTo(b.name).takeIf { it != 0 }  ?: 1
                }
            LibrarySortMode.Count ->
                toSortedMap { a: ContentList, b: ContentList ->
                    this[b]!!.size.compareTo(this[a]!!.size)
                }
            LibrarySortMode.RecentlyAdded -> {
                toSortedMap { a: ContentList, b: ContentList ->
                    b.lastModified.compareTo(a.lastModified)
                }

            }
        }
            .toMap()
            .toList()
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
        data class ListOptions(val contentList: ContentList, val items: ImmutableList<ContentItem>) : Dialog

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
    val contentLists: ImmutableList<Pair<ContentList, ImmutableList<ContentItem>>> = persistentListOf(),
    val favorites: ImmutableList<ContentItem> = persistentListOf(),
    val refreshingLists: Boolean = false,
    val refreshingFavorites: Boolean = false
)