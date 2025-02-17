package io.silv.movie.presentation.screenmodel

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
import io.silv.movie.data.model.ContentPagedType
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.RecommendationManager
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.model.toContentItem
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.data.local.networkToLocalMovie
import io.silv.movie.data.local.networkToLocalShow
import io.silv.movie.data.model.toDomain
import io.silv.movie.data.network.NetworkContentDelegate
import io.silv.movie.data.network.getMoviePager
import io.silv.movie.data.network.getShowPager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
    private val network: NetworkContentDelegate,
    private val local: LocalContentDelegate,
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
                    recommendations = recommendations
                )
            }
        }
            .launchIn(screenModelScope)

        state.map { it.showIds to it.movieIds }
            .distinctUntilChanged()
            .flatMapLatest { (showIds, movieIds) ->
                contentListRepository.observeFavorites(query, FavoritesSortMode.RecentlyAdded)
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
                    )
                }
            }
            .launchIn(screenModelScope)

        contentListRepository.observeListItemsByListId(listId, "", ListSortMode.RecentlyAdded(true))
            .onEach { content ->
                mutableState.updateSuccess {state ->
                    state.copy(listItems = content)
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
            when {
                query.isBlank() -> flowOf(PagingData.empty())
                movies ->  Pager(PagingConfig(pageSize = 25)) {
                    network.getMoviePager(ContentPagedType.Search(query))
                }.flow.map { pagingData ->
                    val seenIds = mutableSetOf<Long>()
                    pagingData.map { sMovie ->
                        local.networkToLocalMovie(sMovie.toDomain())
                            .let { localMovie ->
                                local.observeMoviePartialById(localMovie.id)
                                    .map { it.toContentItem() }
                            }
                            .stateIn(ioCoroutineScope)
                    }
                        .filter { seenIds.add(it.value.contentId) && it.value.posterUrl.isNullOrBlank().not() }
                        .filter { !movieIds.contains(it.value.contentId) }
                }
                else ->
                    Pager(PagingConfig(pageSize = 25)) {
                        network.getShowPager(ContentPagedType.Search(query))
                    }.flow.map { pagingData ->
                        val seenIds = mutableSetOf<Long>()
                        pagingData.map { sShow ->
                            local.networkToLocalShow(sShow.toDomain())
                                .let { localShow ->
                                    local.observeShowPartialById(localShow.id)
                                        .map { it.toContentItem() }
                                }
                                .stateIn(ioCoroutineScope)
                        }
                            .filter { seenIds.add(it.value.contentId) && it.value.posterUrl.isNullOrBlank().not() }
                            .filter { !showIds.contains(it.value.contentId) }
                    }
            }
                .cachedIn(screenModelScope)

        }
        .stateIn(screenModelScope, SharingStarted.Lazily, PagingData.empty())


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
        val listItems: List<ContentItem> = emptyList(),
        val recommendations: List<ContentItem> = emptyList(),
        val favorites: List<ContentItem> = emptyList(),
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