package io.silv.movie.presentation.screenmodel

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
import io.silv.movie.data.local.GetContentPagerFlow
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.data.model.ContentPagedType
import io.silv.movie.data.model.Filters
import io.silv.movie.data.model.Genre
import io.silv.movie.data.model.SearchItem
import io.silv.movie.data.model.ContentItem
import io.silv.movie.data.model.MoviePoster
import io.silv.movie.data.model.toDomain
import io.silv.movie.data.local.MovieRepository
import io.silv.movie.data.local.networkToLocalMovie
import io.silv.movie.data.network.NetworkContentDelegate
import io.silv.movie.data.network.SourceMovieRepository
import io.silv.movie.data.network.getMoviePager
import io.silv.movie.data.supabase.ContentType
import io.silv.movie.prefrences.BrowsePreferences
import io.silv.movie.prefrences.PosterDisplayMode
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
import java.time.LocalDateTime

class MovieScreenModel(
    private val local: LocalContentDelegate,
    private val network: NetworkContentDelegate,
    networkMonitor: NetworkMonitor,
    browsePreferences: BrowsePreferences,
    savedStateContentPagedType: ContentPagedType,
) : StateScreenModel<MovieState>(
    MovieState(
        listing = savedStateContentPagedType
    )
) {
    private val getContentPagerFlow: GetContentPagerFlow = GetContentPagerFlow.create(
        local,
        network
    )

    var displayMode by browsePreferences.browsePosterDisplayMode().asState(screenModelScope)
        private set

    var gridCells by browsePreferences.browseGridCellCount().asState(screenModelScope)
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
            val genres = network.getMovieGenres().map { it.toDomain() }
            mutableState.update { state -> state.copy(genres = genres) }
        }

        snapshotFlow { query }
            .debounce(1000)
            .onEach {
                if (it.isNotBlank()) {
                    mutableState.update { state ->
                        state.copy(listing = ContentPagedType.Search(it))
                    }
                }
            }
            .launchIn(screenModelScope)
    }

    val moviePagerFlowFlow = state.map { it.listing }
        .combine(browsePreferences.browseHideLibraryItems().changes()) { a, b -> a to b }
        .distinctUntilChanged()
        .flatMapLatest { (listing, hideLibraryItems) ->
            getContentPagerFlow(ContentType.Movie, listing, ioCoroutineScope) {
                filter {
                    it.value.posterUrl.isNullOrBlank().not()
                }.filter {
                    !hideLibraryItems || !it.value.favorite
                }
            }
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, PagingData.empty())

    fun changeCategory(category: ContentPagedType) {
        screenModelScope.launch {
            if (category is ContentPagedType.Search && category.query.isBlank()) {
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

    val searchItems = listOf(
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
                        ((s.toIntOrNull() ?: -1) > LocalDateTime.now().year || (s.toIntOrNull()
                            ?: -1) < 1900 || s.any { !it.isDigit() })
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
        data class ContentOptions(val item: ContentItem) : Dialog

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
    val genres: List<Genre> = emptyList(),
    val filters: Filters = Filters.default
) {
    val genreItems =
        genres
            .map { Pair(filters.genres.contains(it), it) }
}


@Immutable
@Stable
data class MovieActions(
    val changeCategory: (ContentPagedType) -> Unit,
    val changeQuery: (String) -> Unit,
    val movieLongClick: (movie: ContentItem) -> Unit,
    val movieClick: (movie: ContentItem) -> Unit,
    val onSearch: (String) -> Unit,
    val setDisplayMode: (PosterDisplayMode) -> Unit,
    val changeGridCellCount: (Int) -> Unit
)