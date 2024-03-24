package io.silv.movie.presentation.library.screenmodels

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.cache.TVShowCoverCache
import io.silv.movie.data.lists.ContentItem
import io.silv.movie.data.lists.ContentList
import io.silv.movie.data.lists.ContentListRepository
import io.silv.movie.data.lists.toUpdate
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.UpdateMovie
import io.silv.movie.data.movie.model.toMovieUpdate
import io.silv.movie.data.prefrences.LibraryPreferences
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.recommendation.RecommendationManager
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.UpdateShow
import io.silv.movie.data.tv.model.toShowUpdate
import io.silv.movie.data.user.ListRepository
import io.silv.movie.data.user.ListUpdateManager
import io.silv.movie.data.user.User
import io.silv.movie.data.user.UserRepository
import io.silv.movie.data.user.toUserListUpdate
import io.silv.movie.presentation.EventProducer
import io.silv.movie.presentation.asState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

class ListViewScreenModel(
    private val contentListRepository: ContentListRepository,
    private val recommendationManager: RecommendationManager,
    private val updateShow: UpdateShow,
    private val updateMovie: UpdateMovie,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val listRepository: ListRepository,
    private val userRepository: UserRepository,
    private val listUpdateManager: ListUpdateManager,
    private val movieCoverCache: MovieCoverCache,
    private val showCoverCache: TVShowCoverCache,
    private val auth: Auth,
    libraryPreferences: LibraryPreferences,

    private val listId: Long,
    private val supabaseId: String,
): StateScreenModel<ListViewState>(ListViewState.Loading),
    EventProducer<ListViewEvent> by EventProducer.default() {

    private val listSortMode = libraryPreferences.sortModeList()

    var query by mutableStateOf("")
        private set

    var listViewDisplayMode by libraryPreferences.listViewDisplayMode().asState(screenModelScope)
        private set

    private var initializeJob: Job? = null

    private var observing = false
    private val mutex = Mutex()

    init {
        initializeList()

        state.map { it.success?.list?.id }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { id ->
                mutex.withLock {
                    if (!observing) {
                        observing = true
                        observeList(id)
                        observeRecommendation(id)
                        observeSearchItems(id)
                        updateUser(state.value.success?.list?.createdBy.orEmpty())
                    }
                }
            }
            .launchIn(screenModelScope)

        listSortMode.changes()
            .onEach { mode ->
                mutableState.updateSuccess {state -> state.copy(sortMode = mode) }
            }
            .launchIn(screenModelScope)
    }

    val refreshingList = state.map { it.success?.list?.supabaseId }
        .filterNotNull()
        .distinctUntilChanged()
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
                    /*** No need to fetch the items again this will be observed in [observeList] */
                    listUpdateManager.refreshList(list.supabaseId.orEmpty())
                }

                ListViewState.Success(
                    list = list,
                    isOwnerMe = list.createdBy == auth.currentUserOrNull()?.id,
                    allItems = items.toImmutableList(),
                    sortMode = listSortMode.get()
                )
            }
        }
    }

    private fun updateUser(userId: String) {
        screenModelScope.launch {
            val user = userRepository.getUser(userId)

            mutableState.updateSuccess { state ->
                state.copy(user = user)
            }

            auth.sessionStatus.onEach { status ->
                val id =  when(status) {
                    is SessionStatus.Authenticated -> status.session.user?.id
                    else -> null
                }
                mutableState.updateSuccess { state ->
                    state.copy(
                        isOwnerMe = id == state.list.createdBy
                    )
                }
            }
                .launchIn(this)
        }
    }

    private fun observeSearchItems(id: Long) {
        screenModelScope.launch {

            state.map { it.success?.sortMode }
                .filterNotNull()
                .distinctUntilChanged()
                .combine(
                    snapshotFlow { query }
                ) { a, b -> a to b }
                .collectLatest { (sortMode, search) ->

                    contentListRepository
                        .observeListItemsByListId(id, search, sortMode)
                        .collect { content ->
                            mutableState.updateSuccess { state ->
                                state.copy(items = content.toImmutableList())
                            }
                        }
                }
        }
    }

    private fun observeRecommendation(id: Long) {
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
            .launchIn(screenModelScope)
    }

    private fun observeList(id: Long) {
        combine(
            contentListRepository.observeListById(id),
            contentListRepository.observeListItemsByListId(id, "", ListSortMode.Title)
        ) { list, items ->

            if (list == null) {
                mutableState.value = ListViewState.Error("No list found")
                return@combine
            } else {
                mutableState.updateSuccess {state ->
                    state.copy(
                        list = list,
                        allItems = items.toImmutableList()
                    )
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

    fun editDescription(description: String) {
        screenModelScope.launch {
            val prev = state.value.success?.list ?: return@launch
            val new = prev.copy(description = description)

            if (new.supabaseId != null) {
                val result = listRepository.updateList(new.toUserListUpdate())
                if (!result)
                    return@launch
            }
            Timber.d(new.toString())

            contentListRepository.updateList(new.toUpdate())
        }
    }

    fun editList(prev: ContentList, name: String) {
        screenModelScope.launch {
            val new = prev.copy(name = name)

            Timber.d(new.toString())

            if (new.supabaseId != null) {
               val result = listRepository.updateList(new.toUserListUpdate())
               if (!result)
                   return@launch
            }
            Timber.d(new.toString())

            contentListRepository.updateList(new.toUpdate())
        }
    }

    fun updateSortMode(sortMode: ListSortMode) {
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

    fun toggleItemFavorite(contentItem: ContentItem) {
        screenModelScope.launch {
            if (contentItem.isMovie) {
                val movie = getMovie.await(contentItem.contentId) ?: return@launch
                val new = movie.copy(favorite = !movie.favorite)
                if(!new.favorite) {
                    movieCoverCache.deleteFromCache(movie)
                }
                updateMovie.await(new.toMovieUpdate())
            } else {
                val show = getShow.await(contentItem.contentId) ?: return@launch
                val new = show.copy(favorite = !show.favorite)
                if(!new.favorite) {
                    showCoverCache.deleteFromCache(show)
                }
                updateShow.await(new.toShowUpdate())
            }
        }
    }

    fun deleteList() {
        screenModelScope.launch {
            val list = state.value.success?.list ?: return@launch
            runCatching {

                if (list.supabaseId != null) {
                    val result = listRepository.deleteList(list.supabaseId)

                    if (!result)
                        return@launch
                }

                contentListRepository.deleteList(list)
            }
                .onSuccess {
                    emitEvent(ListViewEvent.ListDeleted)
                }
        }
    }

    fun removeFromList(contentItem: ContentItem)  {
        screenModelScope.launch {
            val list = state.value.success?.list ?: return@launch

            if (list.supabaseId != null) {
                val result = if (contentItem.isMovie) {
                    listRepository.deleteMovieFromList(contentItem.contentId, list)
                } else {
                    listRepository.deleteShowFromList(contentItem.contentId, list)
                }

                if (!result)
                    return@launch
            }

            if (contentItem.isMovie) {
                contentListRepository.removeMovieFromList(contentItem.contentId, list)
            } else {
                contentListRepository.removeShowFromList(contentItem.contentId, list)
            }
        }
    }


    fun addToList(contentItem: ContentItem) {
        screenModelScope.launch {
            val list = state.value.success?.list ?: return@launch

            if (list.supabaseId != null) {
                val result = if (contentItem.isMovie) {
                    listRepository.addMovieToList(contentItem.contentId, list)
                } else {
                    listRepository.addShowToList(contentItem.contentId, list)
                }

                if (!result)
                    return@launch
            }

            if (contentItem.isMovie) {
                contentListRepository.addMovieToList(contentItem.contentId, list)
            } else {
                contentListRepository.addShowToList(contentItem.contentId, list)
            }
            recommendationManager.removeRecommendation(contentItem, list.id)
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
        val isOwnerMe: Boolean,
        val list: ContentList,
        val allItems: ImmutableList<ContentItem>,
        val sortMode: ListSortMode,
        val user: User? = null,
        val dialog: ListViewScreenModel.Dialog? = null,
        val items: ImmutableList<ContentItem> = persistentListOf(),
        val recommendations: ImmutableList<ContentItem> = persistentListOf(),
        val refreshingRecommendations: Boolean = false,
        val refreshingList: Boolean = false,
    ): ListViewState

    val success: Success?
        get() = this as? Success
}