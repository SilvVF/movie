package io.silv.movie.presentation.list.screenmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.gotrue.Auth
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.content.lists.RecommendationManager
import io.silv.movie.data.content.lists.interactor.DeleteContentList
import io.silv.movie.data.content.lists.repository.ContentListRepository
import io.silv.movie.data.prefrences.BasePreferences
import io.silv.movie.data.prefrences.BasePreferences.Companion.addToRecentlyViewed
import io.silv.movie.data.prefrences.LibraryPreferences
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.user.ListUpdateManager
import io.silv.movie.data.user.User
import io.silv.movie.data.user.repository.UserRepository
import io.silv.movie.presentation.EventProducer
import io.silv.movie.presentation.asState
import io.silv.movie.presentation.covers.cache.MovieCoverCache
import io.silv.movie.presentation.covers.cache.TVShowCoverCache
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ListViewScreenModel(
    private val contentListRepository: ContentListRepository,
    private val recommendationManager: RecommendationManager,
    private val deleteContentList: DeleteContentList,
    private val userRepository: UserRepository,
    private val listUpdateManager: ListUpdateManager,
    private val movieCoverCache: MovieCoverCache,
    private val showCoverCache: TVShowCoverCache,
    private val auth: Auth,
    basePreferences: BasePreferences,
    libraryPreferences: LibraryPreferences,

    private val listId: Long,
    private val supabaseId: String,
): StateScreenModel<ListViewState>(ListViewState.Loading),
    EventProducer<ListViewEvent> by EventProducer.default() {

    private val listSortMode = libraryPreferences.listViewSortMode()

    var query by mutableStateOf("")
        private set

    var listViewDisplayMode by libraryPreferences.listViewDisplayMode().asState(screenModelScope)
        private set

    private val listIdFlow = state.map { it.success?.list?.id }.filterNotNull().distinctUntilChanged()
    private val createdByFlow = state.map { it.success?.list?.createdBy }.filterNotNull().distinctUntilChanged()
    private val listFlow = state.map { it.success?.list }.filterNotNull().distinctUntilChanged()
    private val sortModeFlow = state.map { it.success?.sortMode }.filterNotNull().distinctUntilChanged()

    private var initializeJob: Job? = null

    init {
        initializeList()

        createdByFlow.onEach { userId ->
            val user = userRepository.getUser(userId) ?: return@onEach
            mutableState.updateSuccess { state ->
                state.copy(user = user)
            }
        }
            .launchIn(screenModelScope)

        listIdFlow.flatMapLatest { id ->
            combine(
                recommendationManager.subscribe(id).map { it.take(6) },
                recommendationManager.isRunning(id)
            ) {  recommendations, isRunning ->
                mutableState.updateSuccess { state ->
                    state.copy(
                        refreshingRecommendations = isRunning,
                        recommendations = recommendations.toImmutableList()
                    )
                }
            }
        }
            .launchIn(screenModelScope)

        listFlow.onEach { list ->
            basePreferences.addToRecentlyViewed(list)
        }
            .launchIn(screenModelScope)

       listIdFlow.flatMapLatest { id ->
           sortModeFlow.combine(
               snapshotFlow { query }
           ) { a, b -> a to b }
                .mapLatest { (sortMode, search) ->
                    contentListRepository
                        .observeListItemsByListId(id, search, sortMode)
                        .collect { content ->

                            mutableState.updateSuccess { state ->
                                state.copy(items = content.toImmutableList())
                            }
                        }
                }
        }
           .launchIn(screenModelScope)

        listIdFlow.flatMapLatest { id ->
            combine(
                contentListRepository.observeListById(id),
                contentListRepository.observeListItemsByListId(id, "", ListSortMode.RecentlyAdded(true)),
            ) { list, items ->

                if (list == null) {
                    mutableState.value = ListViewState.Error("No list found")
                    return@combine
                } else {
                    mutableState.updateSuccess { state ->
                        state.copy(
                            list = list,
                            allItems = items.toImmutableList()
                        )
                    }
                }
            }
        }
            .launchIn(screenModelScope)

        listSortMode.changes()
            .onEach { mode ->
                mutableState.updateSuccess { state -> state.copy(sortMode = mode) }
            }
            .launchIn(screenModelScope)
    }

    val refreshingList = createdByFlow
        .flatMapLatest {
            listUpdateManager.isRunning(it)
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false
        )

    fun initializeList() {
        if(initializeJob?.isActive == true)
            return

        initializeJob = ioCoroutineScope.launch {
            var list: ContentList? = null

            if (listId != -1L) {
                list = contentListRepository.getList(listId)
            } else if (supabaseId.isNotBlank()) {
                list = contentListRepository.getListForSupabaseId(supabaseId)
                if (list == null) {
                    val result = listUpdateManager.awaitRefresh(supabaseId).isSuccess

                    if (result) {
                        list = contentListRepository.getListForSupabaseId(supabaseId)
                    }
                }
            }
            mutableState.value = if (list == null) {
                ListViewState.Error("No list found")
            } else {
                val items = contentListRepository.getListItems(list.id)
                if (
                    items.isEmpty() &&
                    list.supabaseId != null &&
                    list.createdBy != auth.currentUserOrNull()?.id) {
                    listUpdateManager.refreshList(list.supabaseId.orEmpty())
                }
                ListViewState.Success(
                    list = list,
                    allItems = items.toImmutableList(),
                    sortMode = listSortMode.get()
                )
            }
        }
    }

    fun updateQuery(q: String) {
        query = q
    }

    fun updateListViewDisplayMode(mode: PosterDisplayMode) {
        listViewDisplayMode = mode
    }


    fun updateSortMode(sortMode: ListSortMode) {
        Timber.d(sortMode.toString())
        screenModelScope.launch {
            listSortMode.set(sortMode)
        }
    }

    fun refreshRecommendations() {
        screenModelScope.launch {
            val listId = state.value.success?.list?.id ?: return@launch
            recommendationManager.refreshListRecommendations(listId)
        }
    }

    fun refreshList() {
        screenModelScope.launch {
            val supabaseId = state.value.success?.list?.supabaseId ?: return@launch
            listUpdateManager.refreshList(supabaseId)
        }
    }

    fun deleteList() {
        screenModelScope.launch {
            val state = state.value.success ?: return@launch
            deleteContentList.await(state.list, movieCoverCache, showCoverCache)
                .onFailure {
                    emitEvent(ListViewEvent.FailedToRemoveListFromNetwork)
                }
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

    @Stable
    sealed interface Dialog {


        @Stable
        data object SortOptions : Dialog

        @Stable
        data object DeleteList : Dialog

        @Stable
        data object ListOptions : Dialog

        @Stable
        data class ContentOptions(val item: ContentItem): Dialog

        @Stable
        data class RemoveFromFavorites(val item: ContentItem): Dialog

        @Stable
        data object FullCover : Dialog
    }
}

sealed interface ListViewEvent {
    data object ListDeleted: ListViewEvent
    data object FailedToRemoveListFromNetwork: ListViewEvent
}

sealed class ListSortMode(open val ascending: Boolean) {

    data class Title(override val ascending: Boolean): ListSortMode(ascending)
    data class RecentlyAdded(override val ascending: Boolean): ListSortMode(ascending)
    data class Movie(override val ascending: Boolean): ListSortMode(ascending)
    data class Show(override val ascending: Boolean): ListSortMode(ascending)
}

sealed interface ListViewState {

    data object Loading: ListViewState

    data class Error(val message: String): ListViewState

    data class Success(
        val user: User? = null,
        val list: ContentList,
        val allItems: ImmutableList<ContentItem>,
        val sortMode: ListSortMode,
        val dialog: ListViewScreenModel.Dialog? = null,
        val items: ImmutableList<ContentItem> = persistentListOf(),
        val recommendations: ImmutableList<ContentItem> = persistentListOf(),
        val refreshingRecommendations: Boolean = false,
        val refreshingList: Boolean = false,
    ): ListViewState

    val success: Success?
        get() = this as? Success
}