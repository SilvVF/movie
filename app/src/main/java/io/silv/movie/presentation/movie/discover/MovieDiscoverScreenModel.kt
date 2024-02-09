package io.silv.movie.presentation.movie.discover

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.data.movie.interactor.GetMovie
import io.silv.data.movie.interactor.GetRemoteMovie
import io.silv.data.movie.interactor.NetworkToLocalMovie
import io.silv.data.movie.model.Genre
import io.silv.data.movie.model.Movie
import io.silv.data.movie.model.MoviePagedType
import io.silv.data.movie.repository.SourceMovieRepository
import io.silv.data.movie.toDomain
import io.silv.data.prefrences.TMDBPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MovieDiscoverScreenModel(
    private val sourceMovieRepository: SourceMovieRepository,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val tmdbPreferences: TMDBPreferences,
    private val getMovie: GetMovie,
    private val getRemoteMovie: GetRemoteMovie
): StateScreenModel<MovieDiscoverState>(MovieDiscoverState()) {

    init {
        ioCoroutineScope.launch {
            val genres = sourceMovieRepository.getSourceGenres()

            withContext(Dispatchers.Main)  {
                mutableState.update { state ->
                    state.copy(genres = genres.map { it.toDomain() }.toImmutableList())
                }
            }
        }
    }

    private val hideLibraryItems = tmdbPreferences.hideLibraryItems().stateIn(screenModelScope)

    val pagingSources=  state.map { it.genres }
        .distinctUntilChanged()
        .map { genres ->
            genres.map {
                Pager(
                    config = PagingConfig(30) ,
                ) {
                    getRemoteMovie.subscribe(MoviePagedType.Discover(listOf(it.name)))
                }
            }
        }
        .map { pagers ->
            pagers.map { pager ->
                pager.flow.map { pagingData ->
                    val seenIds = mutableSetOf<Long>()
                    pagingData.map dataMap@{ sMovie ->
                        networkToLocalMovie.await(sMovie.toDomain())
                            .let { localMovie -> getMovie.subscribe(localMovie.id) }
                            .stateIn(ioCoroutineScope)
                    }
                        .filter { seenIds.add(it.value.id) && it.value.posterUrl.isNullOrBlank().not() }
                        .filter { !hideLibraryItems.value || !it.value.favorite }
                }
                    .cachedIn(ioCoroutineScope)
            }
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, emptyFlow<List<Flow<PagingData<StateFlow<Movie>>>>>())

    fun changeDialog(dialog: Dialog?) {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(dialog = dialog)
            }
        }
    }

    @Stable
    sealed interface Dialog {
        data object CategorySelect: Dialog
    }
}

@Stable
@Immutable
data class MovieDiscoverState(
    val genres: ImmutableList<Genre> = persistentListOf(),
    val movieData: ImmutableMap<Genre, List<Movie>> = persistentMapOf(),
    val tvData: ImmutableMap<Genre, List<String>> = persistentMapOf(),
    val dialog: MovieDiscoverScreenModel.Dialog? = null
) {

    val combinedData: ImmutableMap<Genre, List<ResourceType>>
        get() {
            val data = mutableMapOf<Genre, MutableList<ResourceType>>()
            // initialize an empty mutable list for each genre
            for (genre in genres) {
                data[genre] = mutableListOf()
            }
            for ((genre, movies) in movieData) {
                val list = data[genre] ?: continue
                for (movie in movies) {
                    list.add(RMovie(movie))
                }
            }
            for ((genre, shows) in tvData) {
                val list = data[genre] ?: continue
                for (show in shows) {
                    list.add(RTVShow(show))
                }
            }
            // shuffle values so tv shows are mixed together and dont appear back to back
            data.values.forEach { list ->  list.shuffle() }

            return data.toImmutableMap()
        }
}

@Stable
sealed interface ResourceType
@Stable
data class RMovie(val movie: Movie): ResourceType
@Stable
data class RTVShow(val show: String): ResourceType