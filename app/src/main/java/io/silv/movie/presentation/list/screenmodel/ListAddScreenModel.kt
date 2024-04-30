package io.silv.movie.presentation.list.screenmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.core.Quad
import io.silv.movie.data.content.ContentPagedType
import io.silv.movie.data.content.lists.ContentItem
import io.silv.movie.data.content.lists.ContentList
import io.silv.movie.data.content.lists.RecommendationManager
import io.silv.movie.data.content.lists.interactor.GetFavoritesList
import io.silv.movie.data.content.lists.repository.ContentListRepository
import io.silv.movie.data.content.lists.toContentItem
import io.silv.movie.data.content.movie.interactor.GetMovie
import io.silv.movie.data.content.movie.interactor.GetRemoteMovie
import io.silv.movie.data.content.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.content.movie.model.toDomain
import io.silv.movie.data.content.tv.interactor.GetRemoteTVShows
import io.silv.movie.data.content.tv.interactor.GetShow
import io.silv.movie.data.content.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.content.tv.model.toDomain
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ListAddScreenModel(
    private val contentListRepository: ContentListRepository,
    private val recommendationManager: RecommendationManager,
    private val getFavoritesList: GetFavoritesList,
    private val getRemoteMovie: GetRemoteMovie,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val networkToLocalTVShow: NetworkToLocalTVShow,
    private val getRemoteTVShows: GetRemoteTVShows,
    private val getShow: GetShow,
    private val getMovie: GetMovie,
    private val listId: Long,
): StateScreenModel<ListAddState>(ListAddState.Loading){

    private fun MutableStateFlow<ListAddState>.updateSuccess(
        function: (ListAddState.Success) -> ListAddState.Success
    ) {
        update {
            when (it) {
                is ListAddState.Success -> function(it)
                else -> it
            }
        }
    }

    private val stateSuccessTrigger =
        state.map { it.success?.list?.id }.filterNotNull().distinctUntilChanged()

    var query by mutableStateOf("")
        private set

    init {
        screenModelScope.launch {
            val list = runCatching { contentListRepository.getList(listId) }.getOrNull()
            if (list != null) {
                mutableState.value = ListAddState.Success(list = list)
                refreshRecommendations()
            } else {
                mutableState.value = ListAddState.Error("No list found")
            }
        }

        combine(
            stateSuccessTrigger,
            recommendationManager.subscribe(listId),
            recommendationManager.isRunning(listId)
        ) { _, recommendations, isRunning ->
            mutableState.updateSuccess { state ->
                state.copy(
                    refreshingRecommendations = isRunning,
                    recommendations = recommendations.toImmutableList()
                )
            }
        }
            .launchIn(screenModelScope)

        state.map { it.showIds to it.movieIds }
            .distinctUntilChanged()
            .flatMapLatest { (showIds, movieIds) ->
                getFavoritesList.subscribe(query = query, FavoritesSortMode.RecentlyAdded)
                    .map { items ->
                        items.filterNot {
                            if (it.isMovie)
                                movieIds.contains(it.contentId)
                            else
                                showIds.contains(it.contentId)
                        }
                    }
            }
            .onEach { content ->
                mutableState.updateSuccess {state ->
                    state.copy(
                        favorites = content.filter {
                            if (it.isMovie) it.contentId !in state.movieIds
                            else it.contentId !in state.showIds
                        }
                            .toImmutableList()
                    )
                }
            }
            .launchIn(screenModelScope)

        contentListRepository.observeListItemsByListId(listId, "", ListSortMode.RecentlyAdded(true))
            .onEach { content ->
                mutableState.updateSuccess {state ->
                    state.copy(listItems = content.toImmutableList())
                }
            }
            .launchIn(screenModelScope)
    }

    fun updateQuery(q: String) {
        query = q
    }


    val contentPagingFlow = combine(
        snapshotFlow { query },
        state.map { it.movieIds },
        state.map { it.showIds },
        state.map { it.success?.movies ?: true }
    ) { a, b, c, d -> Quad(a, b, c, d) }
        .debounce(300)
        .flatMapLatest { (query, movieIds, showIds, movies) ->

            if (query.isBlank())
                return@flatMapLatest flowOf(PagingData.empty<StateFlow<ContentItem>>())

            if (movies) {
                Pager(PagingConfig(pageSize = 25)) {
                    getRemoteMovie.subscribe(ContentPagedType.Search(query))
                }.flow.map { pagingData ->
                    val seenIds = mutableSetOf<Long>()
                    pagingData.map { sMovie ->
                        networkToLocalMovie.await(sMovie.toDomain())
                            .let { localMovie ->
                                getMovie.subscribePartial(localMovie.id)
                                    .map { it.toContentItem() }
                            }
                            .stateIn(ioCoroutineScope)
                    }
                        .filter {
                            seenIds.add(it.value.contentId) && it.value.posterUrl.isNullOrBlank()
                                .not()
                        }
                        .filter { !movieIds.contains(it.value.contentId) }
                }
            } else {
                Pager(PagingConfig(pageSize = 25)) {
                    getRemoteTVShows.subscribe(ContentPagedType.Search(query))
                }.flow.map { pagingData ->
                    val seenIds = mutableSetOf<Long>()
                    pagingData.map { sShow ->
                        networkToLocalTVShow.await(sShow.toDomain())
                            .let { localShow ->
                                getShow.subscribePartial(localShow.id)
                                    .map { it.toContentItem() }
                            }
                            .stateIn(ioCoroutineScope)
                    }
                        .filter { seenIds.add(it.value.contentId) && it.value.posterUrl.isNullOrBlank().not() }
                        .filter { !showIds.contains(it.value.contentId) }

                }
            }
                .cachedIn(ioCoroutineScope)
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, PagingData.empty())


    fun changePagingItems() {
        screenModelScope.launch {
            mutableState.updateSuccess {state ->
                state.copy(movies = !state.movies)
            }
        }
    }


    fun refreshRecommendations() {
        screenModelScope.launch {
            recommendationManager.refreshListRecommendations(listId, amount = 100, perItem = 10)
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
        data class RemoveFromFavorites(val item: ContentItem): Dialog
    }

}


@Stable
sealed interface ListAddState {

    @Immutable
    data object Loading: ListAddState

    @Immutable
    data class Error(val message: String): ListAddState

    @Immutable
    data class Success(
        val list: ContentList,
        val movies: Boolean = true,
        val listItems: ImmutableList<ContentItem> = persistentListOf(),
        val recommendations: ImmutableList<ContentItem> = persistentListOf(),
        val favorites: ImmutableList<ContentItem> = persistentListOf(),
        val refreshingRecommendations: Boolean = false,
        val dialog: ListAddScreenModel.Dialog? = null
    ): ListAddState

    val success: Success?
        get() = this as? Success

    val movieIds: Set<Long>
        get() = success?.listItems?.filter { it.isMovie }?.map { it.contentId }?.toSet() ?: emptySet()

    val showIds: Set<Long>
        get() = success?.listItems?.filterNot { it.isMovie }?.map { it.contentId }?.toSet() ?: emptySet()
}