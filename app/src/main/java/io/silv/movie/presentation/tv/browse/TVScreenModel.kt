package io.silv.movie.presentation.tv.browse

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.data.movie.interactor.GetRemoteTVShows
import io.silv.movie.data.movie.model.ContentPagedType
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.prefrences.TMDBPreferences
import io.silv.movie.data.tv.TVShow
import io.silv.movie.data.tv.interactor.GetShow
import io.silv.movie.data.tv.interactor.NetworkToLocalTVShow
import io.silv.movie.data.tv.interactor.UpdateShow
import io.silv.movie.data.tv.toDomain
import io.silv.movie.data.tv.toShowUpdate
import io.silv.movie.presentation.asState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TVScreenModel(
    private val getRemoteTVShow: GetRemoteTVShows,
    private val networkToLocalMovie: NetworkToLocalTVShow,
    private val getShow: GetShow,
    private val updateMovie: UpdateShow,
    tmdbPreferences: TMDBPreferences,
    query: String
) : StateScreenModel<TVState>(
    TVState(
        listing = if (query.isNotBlank()) {
            ContentPagedType.Search(query)
        } else {
            ContentPagedType.Default.Popular
        }
    )
) {
    var displayMode by tmdbPreferences.sourceDisplayMode().asState(screenModelScope)
        private set

    var gridCells by tmdbPreferences.gridCellsCount().asState(screenModelScope)
        private set

    init {
        state.map { it.query }
            .debounce(1000)
            .onEach {
                if (it.isNotBlank()) {
                    mutableState.update {state ->
                        state.copy(listing = ContentPagedType.Search(it))
                    }
                }
            }
            .launchIn(screenModelScope)
    }

    val tvPagerFlowFlow = state.map { it.listing }
        .combine(tmdbPreferences.hideLibraryItems().changes()) { a, b -> a to b}
        .distinctUntilChanged()
        .flatMapLatest { (listing, hideLibraryItems) ->
            Pager(
                PagingConfig(pageSize = 25)
            ) {
                getRemoteTVShow.subscribe(listing)
            }.flow.map { pagingData ->
                val seenIds = mutableSetOf<Long>()
                pagingData.map { sTVShow ->
                    networkToLocalMovie.await(sTVShow.toDomain())
                        .let { localShow -> getShow.subscribe(localShow.id) }
                        .stateIn(ioCoroutineScope)
                }
                    .filter { seenIds.add(it.value.id) && it.value.posterUrl.isNullOrBlank().not() }
                    .filter { !hideLibraryItems || !it.value.favorite }
            }
                .cachedIn(ioCoroutineScope)
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, PagingData.empty())

    fun changeCategory(category: ContentPagedType) {
        screenModelScope.launch {
            if(category is ContentPagedType.Search && category.query.isBlank()) {
                return@launch
            }
            mutableState.update { state -> state.copy(listing = category) }
        }
    }

    fun changeQuery(query: String) {
        screenModelScope.launch {
            mutableState.update { state -> state.copy(query = query) }
        }
    }


    fun changeDisplayMode(mode: PosterDisplayMode) {
        screenModelScope.launch {
            displayMode = mode
        }
    }

    fun changeDialog(dialog: Dialog?) {
        screenModelScope.launch {
            mutableState.update {state ->
                state.copy(dialog = dialog)
            }
        }
    }

    fun changeGridCells(count: Int) {
        screenModelScope.launch { gridCells = count }
    }

    fun onSearch(query: String) {
        screenModelScope.launch {
            if (query.isBlank()) {
                return@launch
            }
            mutableState.update { state -> state.copy(listing = ContentPagedType.Search(query)) }
        }
    }

    fun toggleShowFavorite(tvShow: TVShow) {
        screenModelScope.launch {
            val update = tvShow.copy(favorite = !tvShow.favorite).toShowUpdate()

            updateMovie.await(update)
        }
    }


    @Stable
    sealed interface Dialog {

        @Stable
        data object Filter : Dialog

        @Stable
        data class RemoveShow(val show: TVShow) : Dialog
    }

}

@Immutable
@Stable
data class TVState(
    val listing: ContentPagedType = ContentPagedType.Default.Popular,
    val query: String = "",
    val dialog: TVScreenModel.Dialog? = null,
)


@Immutable
@Stable
data class TVActions(
    val changeCategory: (ContentPagedType) -> Unit,
    val changeQuery: (String) -> Unit,
    val showLongClick: (tvShow: TVShow) -> Unit,
    val showClick: (tvShow: TVShow) -> Unit,
    val onSearch: (String) -> Unit,
    val setDisplayMode: (PosterDisplayMode) -> Unit,
    val changeGridCellCount: (Int) -> Unit
)