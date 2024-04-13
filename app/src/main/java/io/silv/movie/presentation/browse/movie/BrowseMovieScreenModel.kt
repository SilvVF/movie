package io.silv.movie.presentation.browse.movie

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.KeyboardType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.core.NetworkMonitor
import io.silv.movie.data.ContentPagedType
import io.silv.movie.data.Filters
import io.silv.movie.data.Genre
import io.silv.movie.data.SearchItem
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.GetRemoteMovie
import io.silv.movie.data.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.movie.model.MoviePoster
import io.silv.movie.data.movie.model.toDomain
import io.silv.movie.data.movie.repository.SourceMovieRepository
import io.silv.movie.data.prefrences.PosterDisplayMode
import io.silv.movie.data.prefrences.TMDBPreferences
import io.silv.movie.data.toDomain
import io.silv.movie.presentation.asState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
import java.time.LocalDateTime

class MovieScreenModel(
    private val getRemoteMovie: GetRemoteMovie,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val getMovie: GetMovie,
    private val sourceRepository: SourceMovieRepository,
    networkMonitor: NetworkMonitor,
    tmdbPreferences: TMDBPreferences,
    savedStateContentPagedType: ContentPagedType
) : StateScreenModel<MovieState>(
    MovieState(
        listing = savedStateContentPagedType
    )
) {
    var displayMode by tmdbPreferences.sourceDisplayMode().asState(screenModelScope)
        private set

    var gridCells by tmdbPreferences.gridCellsCount().asState(screenModelScope)
        private set

    var query by mutableStateOf("")
        private set

    val online = networkMonitor.isOnline
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            true
        )


    init {
        screenModelScope.launch {
            val genres = sourceRepository.getSourceGenres().map { it.toDomain() }.toImmutableList()
            mutableState.update {state -> state.copy(genres = genres) }
        }

        snapshotFlow { query }
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

    val moviePagerFlowFlow = state.map { it.listing }
        .combine(tmdbPreferences.hideLibraryItems().changes()) { a, b -> a to b}
        .distinctUntilChanged()
        .flatMapLatest { (listing, hideLibraryItems) ->
            Pager(
                PagingConfig(pageSize = 25)
            ) {
                getRemoteMovie.subscribe(listing)
            }.flow.map { pagingData ->
                val seenIds = mutableSetOf<Long>()
                pagingData.map { sMovie ->
                    networkToLocalMovie.await(sMovie.toDomain())
                        .let { localMovie -> getMovie.subscribePartial(localMovie.id) }
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
        this.query = query
    }

    fun changeDisplayMode(mode: PosterDisplayMode) {
        screenModelScope.launch {
            displayMode = mode
        }
    }

    fun changeDialog(dialog: Dialog?) {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(dialog = dialog)
            }
        }
    }

    fun resetFilters() {
        screenModelScope.launch {
            mutableState.update { state -> state.copy(filters = Filters.default) }
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

    fun changeFilters(update: (Filters) -> Filters) {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(filters = update(state.filters))
            }
        }
    }

    val searchItems = persistentListOf(
        SearchItem(
            label = "Companies",
            text = state.value.filters.companies
        ),
        SearchItem(
            label = "Keywords",
            text = state.value.filters.keywords
        ),
        SearchItem(
            label = "People",
            text = state.value.filters.people,
        ),
        SearchItem(
            label = "Year",
            text = state.value.filters.year,
            error = derivedStateOf {
                state.value.filters.year.value
                    .takeIf { s ->
                        ((s.toIntOrNull() ?: -1) > LocalDateTime.now().year || (s.toIntOrNull() ?: -1) < 1900 || s.any { !it.isDigit() })
                                && s.isNotBlank()
                    }
                    ?.let { "Year must be between 1900 and the current year" }
            },
            keyboardType = KeyboardType.NumberPassword
        ),
        SearchItem(
            label = "Vote count",
            text = state.value.filters.voteCount,
            error = derivedStateOf {
                state.value.filters.voteCount.value
                    .takeIf { s ->
                        ((s.toFloatOrNull() ?: -1f) < 0f || s.any { !it.isDigit() && it != '.' })
                                && s.isNotBlank()
                    }
                    ?.let { "Vote count must be a number >= 0" }
            },
            keyboardType = KeyboardType.Number
        ),
        SearchItem(
            label = "Vote average",
            text = state.value.filters.voteAverage,
            error = derivedStateOf {
                state.value.filters.voteAverage.value
                    .takeIf { s ->
                        ((s.toFloatOrNull() ?: -1f) < 0f || s.any { !it.isDigit() && it != '.' })
                                && s.isNotBlank()
                    }
                    ?.let { "Vote average must be a number >= 0" }
            },
            keyboardType = KeyboardType.Number
        ),
    )


    @Stable
    sealed interface Dialog {

        @Stable
        data object Filter : Dialog

        @Stable
        data class RemoveMovie(val movie: MoviePoster) : Dialog
    }
}

@Immutable
@Stable
data class MovieState(
    val listing: ContentPagedType = ContentPagedType.Default.Popular,
    val dialog: MovieScreenModel.Dialog? = null,
    val genres: ImmutableList<Genre> = persistentListOf(),
    val filters: Filters = Filters.default
) {
    val genreItems =
        genres
            .map { Pair(filters.genres.contains(it), it) }
            .toImmutableList()
}


@Immutable
@Stable
data class MovieActions(
    val changeCategory: (ContentPagedType) -> Unit,
    val changeQuery: (String) -> Unit,
    val movieLongClick: (movie: MoviePoster) -> Unit,
    val movieClick: (movie: MoviePoster) -> Unit,
    val onSearch: (String) -> Unit,
    val setDisplayMode: (PosterDisplayMode) -> Unit,
    val changeGridCellCount: (Int) -> Unit
)