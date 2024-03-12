package io.silv.movie.presentation.library.browse

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

class LibraryScreenModel(
    private val contentListRepository: ContentListRepository,
    preferences: LibraryPreferences,
    getFavoritesList: GetFavoritesList
):  StateScreenModel<LibraryState>(LibraryState()),
    EventProducer<LibraryEvent> by EventProducer.default() {

    var query by mutableStateOf("")
        private set

    private val queryFlow = snapshotFlow { query }

    var displayInList by preferences.displayInList().asState(screenModelScope)
        private set

    var sortMode by preferences.sortMode().asState(screenModelScope)
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
                    .groupBy { item -> item.list }
                    .mapValues { (_, items) -> items.toImmutableList() }
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

    fun createList(name: String) {
        screenModelScope.launch {
            val id = contentListRepository.createList(name)
            emitEvent(LibraryEvent.ListCreated(id))
        }
    }

    private fun Map<ContentList, ImmutableList<ContentListItem>>.applySorting(
        sortMode: LibrarySortMode
    ): List<Pair<ContentList, ImmutableList<ContentListItem>>> {
        return when (sortMode) {
            LibrarySortMode.Title ->
                toSortedMap { a: ContentList, b: ContentList ->
                    a.name.compareTo(b.name).takeIf { it != 0 }  ?: 1
                }
                    .toList()
            LibrarySortMode.Count ->
                toList()
                    .sortedByDescending { (_, value) ->
                        value.filterIsInstance<ContentListItem.Item>().size
                    }
            LibrarySortMode.RecentlyAdded -> {
                toSortedMap { a: ContentList, b: ContentList ->
                    (b.lastModified - a.lastModified).toInt().takeIf { it != 0 }  ?: 1
                }
                    .toList()
            }
        }
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
    val contentLists: ImmutableList<Pair<ContentList, ImmutableList<ContentListItem>>> = persistentListOf(),
    val favorites: ImmutableList<ContentItem> = persistentListOf()
)