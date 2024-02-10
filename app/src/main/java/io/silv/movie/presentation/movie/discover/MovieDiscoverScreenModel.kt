package io.silv.movie.presentation.movie.discover

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams.Refresh
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.core.SMovie
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.data.movie.interactor.CombinedPagingSource
import io.silv.data.movie.interactor.GetMovie
import io.silv.data.movie.interactor.GetRemoteMovie
import io.silv.data.movie.interactor.GetRemoteTVShows
import io.silv.data.movie.interactor.NetworkToLocalMovie
import io.silv.data.movie.model.Genre
import io.silv.data.movie.model.Movie
import io.silv.data.movie.model.MoviePagedType
import io.silv.data.movie.repository.SourceMovieRepository
import io.silv.data.movie.toDomain
import io.silv.data.prefrences.TMDBPreferences
import io.silv.movie.presentation.movie.browse.Resource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class MovieDiscoverScreenModel(
    private val sourceMovieRepository: SourceMovieRepository,
    private val tmdbPreferences: TMDBPreferences,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val getMovie: GetMovie,
    private val getRemoteMovie: GetRemoteMovie,
    private val getRemoteTVShows: GetRemoteTVShows,
    genre: Genre?,
    resource: Resource?,
): StateScreenModel<MovieDiscoverState>(
    MovieDiscoverState(selectedGenre = genre, selectedResource = resource)
) {

    init {
        ioCoroutineScope.launch {
            val genres = sourceMovieRepository.getSourceGenres()

            withContext(Dispatchers.Main) {
                mutableState.update { state ->
                    state.copy(genres = genres.map { it.toDomain() }.toImmutableList())
                }
            }
        }

        state.map { it.genres }
            .distinctUntilChanged()
            .map { genres ->
                supervisorScope {
                    genres.associateWith { genre ->
                        loadMovieAndShowData(1, genre)
                    }.map { (genre, moviesDeffered) ->

                        val seenIds = mutableSetOf<Long>()

                        launch {
                            val sourceMovies =  moviesDeffered.await()

                            val movies = sourceMovies.map { sMovie ->
                                networkToLocalMovie.await(sMovie.toDomain())
                                    .let { localMovie -> getMovie.subscribe(localMovie.id) }
                                    .stateIn(ioCoroutineScope)
                            }
                                .filter { movie -> seenIds.add(movie.value.id) }
                                .filter { movie -> movie.value.posterUrl.isNullOrBlank().not() }
                                .filter { !hideLibraryItems.value || !it.value.favorite }

                            withContext(Dispatchers.Main) {
                                mutableState.update { state ->
                                    state.copy(
                                        genreWithMovie =
                                        (state.genreWithMovie + (genre to movies.toImmutableList()))
                                            .toImmutableList()
                                    )
                                }
                            }
                        }
                    }
                }
            }
            .launchIn(ioCoroutineScope)
    }

    private suspend fun loadMovieAndShowData(page: Long?, genre: Genre): Deferred<List<SMovie>> {
        return withContext(Dispatchers.IO) {

            val movieSource =
                getRemoteMovie.subscribe(MoviePagedType.Discover(listOf(genre.name)))
            val tvSource =
                getRemoteTVShows.subscribe(MoviePagedType.Discover(listOf(genre.name)))

            async {
                val (movie, tv) = listOf(
                    async { movieSource.load(Refresh(key = page, loadSize = 30, false)) },
                    async { tvSource.load(Refresh(key = page, loadSize = 30, false)) }
                )
                    .awaitAll()

                listOfNotNull(
                    movie as? PagingSource.LoadResult.Page,
                    tv as? PagingSource.LoadResult.Page
                )
                    .flatMap { it.data }
                    .shuffled()
            }
        }
    }

    private fun getPagingSourceOrNull(genre: Genre?, resource: Resource?): Pager<Long, SMovie>? {
        if (genre == null && resource == null) {
            return null
        }
        return Pager(
            config = PagingConfig(pageSize = 50)
        ) {
            val genres = genre?.name?.let { listOf(it) } ?: state.value.genres.map { it.name }
            when {
                resource != null -> {
                    when (resource) {
                        Resource.Movie -> getRemoteMovie.subscribe(MoviePagedType.Discover(genres))
                        Resource.TVShow -> getRemoteTVShows.subscribe(MoviePagedType.Discover(genres))
                    }
                }
                else -> {
                    CombinedPagingSource(
                        listOf(
                            getRemoteMovie.subscribe(MoviePagedType.Discover(genres)),
                            getRemoteTVShows.subscribe(MoviePagedType.Discover(genres))
                        )
                    )
                }
            }
        }
    }


    private val hideLibraryItems = tmdbPreferences.hideLibraryItems().stateIn(screenModelScope)

    val pagingFlow = state.map { it.selectedGenre to it.selectedResource }
        .distinctUntilChanged()
        .map { (genre, resource) ->
            val source = getPagingSourceOrNull(genre, resource) ?: return@map null

            source.flow.map { pagingData ->
                val seenIds = mutableSetOf<Long>()
                pagingData.map { sMovie ->
                    networkToLocalMovie.await(sMovie.toDomain())
                        .let { localMovie -> getMovie.subscribe(localMovie.id) }
                        .stateIn(ioCoroutineScope)
                }
                    .filter { seenIds.add(it.value.id) && it.value.posterUrl.isNullOrBlank().not() }
                    .filter { !hideLibraryItems.value || !it.value.favorite }
            }
                .cachedIn(ioCoroutineScope)
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, null)

    fun changeDialog(dialog: Dialog?) {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(dialog = dialog)
            }
        }
    }

    fun clearAllSelection() {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(
                    selectedGenre = null,
                    selectedResource = null
                )
            }
        }
    }

    fun onGenreSelected(genre: Genre?) {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(selectedGenre = genre)
            }
        }
    }

    fun onResourceSelected(resource: Resource?) {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(selectedResource = resource)
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
    val dialog: MovieDiscoverScreenModel.Dialog? = null,
    val genreWithMovie: ImmutableList<Pair<Genre, ImmutableList<StateFlow<Movie>>>> = persistentListOf(),
    val selectedGenre: Genre? = null,
    val selectedResource: Resource? = null
)
