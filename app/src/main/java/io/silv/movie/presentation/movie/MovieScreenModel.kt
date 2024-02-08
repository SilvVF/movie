package io.silv.movie.presentation.movie

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.data.movie.interactor.GetMovie
import io.silv.data.movie.interactor.GetRemoteMovie
import io.silv.data.movie.interactor.NetworkToLocalMovie
import io.silv.data.movie.model.Movie
import io.silv.data.movie.model.MoviePagedType
import io.silv.data.movie.toDomain
import io.silv.data.prefrences.PosterDisplayMode
import io.silv.data.prefrences.TMDBPreferences
import io.silv.movie.presentation.asState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class MovieScreenModel(
    private val getRemoteMovie: GetRemoteMovie,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val getMovie: GetMovie,
    tmdbPreferences: TMDBPreferences,
    query: String
) : StateScreenModel<MovieState>(
    MovieState(
        listing = if (query.isNotBlank()) {
            MoviePagedType.Search(query)
        } else {
            MoviePagedType.Default.Popular
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
                        state.copy(listing = MoviePagedType.Search(it))
                    }
                }
            }
            .launchIn(screenModelScope)
    }

    val moviePagerFlowFlow = state.map { it.listing }
        .combine(tmdbPreferences.hideLibraryItems().changes()) { a, b -> a to b}
        .distinctUntilChanged()
        .map { (listing, hideLibraryItems) ->
            Pager(
                PagingConfig(pageSize = 25)
            ) {
                getRemoteMovie.subscribe(listing)
            }.flow.map { pagingData ->
                val seenIds = mutableSetOf<Long>()
                pagingData.map dataMap@{ sMovie ->
                    networkToLocalMovie.await(sMovie.toDomain())
                        .let { localMovie -> getMovie.subscribe(localMovie.id) }
                        .stateIn(ioCoroutineScope)
                }
                    .filter { seenIds.add(it.value.id) && it.value.posterUrl.isNullOrBlank().not() }
                    .filter { !hideLibraryItems || !it.value.favorite }
            }
                .cachedIn(ioCoroutineScope)
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, emptyFlow())

    fun changeCategory(category: MoviePagedType) {
        screenModelScope.launch {
            if(category is MoviePagedType.Search && category.query.isBlank()) {
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

    fun changeResource(resource: Resource) {
       screenModelScope.launch {
           mutableState.update {state -> state.copy(resource = resource)
           }
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
            mutableState.update { state -> state.copy(listing = MoviePagedType.Search(query)) }
        }
    }

    @Stable
    sealed interface Dialog {

        @Stable
        data object Filter : Dialog

        @Stable
        data class RemoveMovie(val movie: Movie) : Dialog
    }

}
@Immutable
@Stable
data class MovieState(
    val listing: MoviePagedType = MoviePagedType.Default.Popular,
    val query: String = "",
    val resource: Resource = Resource.Movie,
    val dialog: MovieScreenModel.Dialog? = null,
)

@Stable
@Immutable
enum class Resource {
    @Stable
    Movie { override fun toString(): String = "Movies" },
    @Stable
    TVShow  { override fun toString(): String = "TV Shows" }
}

@Immutable
@Stable
data class MovieActions(
    val changeCategory: (MoviePagedType) -> Unit,
    val changeResource: (Resource) -> Unit,
    val changeQuery: (String) -> Unit,
    val movieLongClick: (movie: Movie) -> Unit,
    val movieClick: (movie: Movie) -> Unit,
    val onSearch: (String) -> Unit,
    val setDisplayMode: (PosterDisplayMode) -> Unit,
    val changeGridCellCount: (Int) -> Unit
)