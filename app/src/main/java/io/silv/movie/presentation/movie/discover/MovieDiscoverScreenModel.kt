package io.silv.movie.presentation.movie.discover

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams.Refresh
import androidx.paging.cachedIn
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.core.SMovie
import io.silv.core.STVShow
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.data.movie.interactor.CombinedPagingSource
import io.silv.data.movie.interactor.GetMovie
import io.silv.data.movie.interactor.GetRemoteMovie
import io.silv.data.movie.interactor.GetRemoteTVShows
import io.silv.data.movie.interactor.GetShow
import io.silv.data.movie.interactor.NetworkToLocalMovie
import io.silv.data.movie.interactor.NetworkToLocalTVShow
import io.silv.data.movie.interactor.UpdateMovie
import io.silv.data.movie.interactor.UpdateShow
import io.silv.data.movie.model.ContentPagedType
import io.silv.data.movie.model.Genre
import io.silv.data.movie.model.Movie
import io.silv.data.movie.model.TVShow
import io.silv.data.movie.model.toMovieUpdate
import io.silv.data.movie.model.toShowUpdate
import io.silv.data.movie.repository.SourceMovieRepository
import io.silv.data.movie.repository.SourceTVRepository
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

class MovieDiscoverScreenModel(
    private val sourceMovieRepository: SourceMovieRepository,
    private val sourceTVRepository: SourceTVRepository,
    private val tmdbPreferences: TMDBPreferences,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val getMovie: GetMovie,
    private val getShow: GetShow,
    private val getRemoteMovie: GetRemoteMovie,
    private val getRemoteTVShows: GetRemoteTVShows,
    private val networkToLocalTVShow: NetworkToLocalTVShow,
    private val updateMovie: UpdateMovie,
    private val updateShow: UpdateShow,
    genre: Genre?,
    resource: Resource?,
): StateScreenModel<MovieDiscoverState>(
    MovieDiscoverState(selectedGenre = genre, selectedResource = resource)
) {

    init {
        screenModelScope.launch {
            val movieGenres = sourceMovieRepository.getSourceGenres().map { it.toDomain() }
            val tvGenres = sourceTVRepository.getSourceGenres().map { it.toDomain() }

            mutableState.update {
                it.copy(
                    tvGenres = tvGenres.toImmutableList(),
                    movieGenres = movieGenres.toImmutableList()
                )
            }
        }

        state.map { it.genres }
            .distinctUntilChanged()
            .map { genres ->
                supervisorScope {
                    genres.associateWith { genre ->
                        loadContent(1, genre)
                    }.map { (genre,content) ->
                        launch {
                            val seenIds = mutableSetOf<Long>()

                            val  (sourceMovies, sourceTVShows) = content.await()

                            val movies = sourceMovies
                                .map { sMovie -> sMovie.toContentFlow() }
                                .applyDefaultFilters(seenIds)

                            val shows = sourceTVShows
                                .map { sShow -> sShow.toContentFlow() }
                                .applyDefaultFilters(seenIds)

                            val combinedContent = genre to (movies + shows)
                                .shuffled(random = Random(seed = Clock.System.now().toLocalDateTime(TimeZone.UTC).dayOfYear))
                                .toImmutableList()

                            withContext(Dispatchers.Main) {
                                mutableState.update { state ->

                                    val newContent = state.genreWithContent + combinedContent

                                    state.copy(
                                        genreWithContent = newContent
                                            .sortedBy { (genre, _) -> genre.name }
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

    private suspend fun loadContent(page: Long?, genre: Genre): Deferred<Pair<List<SMovie>, List<STVShow>>> {
        return withContext(Dispatchers.IO) {

            val movieGenres = sourceMovieRepository.getSourceGenres().map { it.toDomain() }
            val tvGenres = sourceTVRepository.getSourceGenres().map { it.toDomain() }

            val movieSource =
                getRemoteMovie.subscribe(ContentPagedType.Discover(listOf(genre.name)))
            val tvSource =
                getRemoteTVShows.subscribe(ContentPagedType.Discover(listOf(genre.name)))

            async {
                val tv = if(tvGenres.contains(genre)){
                    async { tvSource.load(Refresh(key = page, loadSize = 30, false)) }
                } else null
                    val movie = if(movieGenres.contains(genre)) {
                        async { movieSource.load(Refresh(key = page, loadSize = 30, false)) }
                } else null

               listOfNotNull(tv, movie).awaitAll()

                Pair(
                    (movie?.getCompleted() as? PagingSource.LoadResult.Page)?.data ?: emptyList(),
                    (tv?.getCompleted() as? PagingSource.LoadResult.Page)?.data  ?: emptyList()
                )
            }
        }
    }

    private fun getPagingDataFlow(genre: Genre?, resource: Resource?): Flow<PagingData<StateFlow<Content>>>? {
        if (genre == null && resource == null) {
            return null
        }
        val genres = genre?.name?.let { listOf(it) } ?: state.value.genres.map { it.name }
        val config =  PagingConfig(50)
        return when {
            resource != null -> {
                when (resource) {
                    Resource.Movie -> Pager(config) {
                        getRemoteMovie.subscribe(ContentPagedType.Discover(genres))
                    }.flow.map {pagingData ->
                        pagingData.map { movie ->
                            movie.toContentFlow()
                        }
                    }
                    Resource.TVShow ->  Pager(config) {
                        getRemoteTVShows.subscribe(ContentPagedType.Discover(genres))
                    }.flow.map { pagingData ->
                        pagingData.map { show ->
                            show.toContentFlow()
                        }
                    }
                }
            }
            else -> {
                Pager(config) {
                    CombinedPagingSource(
                        listOf(
                            getRemoteMovie.subscribe(ContentPagedType.Discover(genres)),
                            getRemoteTVShows.subscribe(ContentPagedType.Discover(genres))
                        ) as List<PagingSource<Long, Any>>
                    )
                }.flow.map { pagingData ->
                    pagingData.map { any ->
                        when(any) {
                            is STVShow -> any.toContentFlow()
                            is SMovie -> any.toContentFlow()
                            else -> error("Unsupported paging type")
                        }
                    }
                }
            }
        }
    }


    private val hideLibraryItems = tmdbPreferences.hideLibraryItems().stateIn(screenModelScope)

    val pagingFlow = state.map { it.selectedGenre to it.selectedResource }
        .distinctUntilChanged()
        .map { (genre, resource) ->
            val flow = getPagingDataFlow(genre, resource) ?: return@map null

            flow.cachedIn(ioCoroutineScope)
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, null)

    fun changeDialog(dialog: Dialog?) {
        screenModelScope.launch {
            mutableState.update { state ->
                state.copy(dialog = dialog)
            }
        }
    }

    fun toggleMovieFavorite(movie: Movie) {
        screenModelScope.launch {
            Log.d("Updating", movie.toString())
            val update = movie.copy(favorite = !movie.favorite).toMovieUpdate()

            updateMovie.await(update)
        }
    }

    fun toggleShowFavorite(show: TVShow) {
        screenModelScope.launch {
            Log.d("Updating", show.toString())
            val update = show.copy(favorite = !show.favorite).toShowUpdate()

            updateShow.await(update)
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
        @Stable
        data object CategorySelect: Dialog
        @Stable
        data class RemoveMovie(val movie: Movie): Dialog
        @Stable
        data class RemoveShow(val show: TVShow): Dialog
    }

    private suspend fun STVShow.toContentFlow(): StateFlow<Content.CShow>  {
        return networkToLocalTVShow.await(toDomain())
            .let { localShow -> getShow.subscribeOrNull(localShow.id) }
            .filterNotNull()
            .catch { e -> e.printStackTrace() }
            .map { Content.CShow(it) }
            .stateIn(ioCoroutineScope)
    }

    private suspend fun SMovie.toContentFlow(): StateFlow<Content.CMovie>  {
        return networkToLocalMovie.await(toDomain())
            .let { localMovie -> getMovie.subscribeOrNull(localMovie.id) }
            .filterNotNull()
            .catch { e -> e.printStackTrace() }
            .map { Content.CMovie(it) }
            .stateIn(ioCoroutineScope)
    }

    @JvmName("applyDefaultFiltersMovie")
    private fun List<StateFlow<Content.CMovie>>.applyDefaultFilters(
        seenIds: MutableSet<Long> = mutableSetOf()) =
        this.filter { c -> seenIds.add(c.value.movie.id) }
            .filter { c -> c.value.movie.posterUrl.isNullOrBlank().not() }
            .filter { c -> !hideLibraryItems.value || !c.value.movie.favorite }

    @JvmName("applyDefaultFiltersShow")
    private fun List<StateFlow<Content.CShow>>.applyDefaultFilters(
        seenIds: MutableSet<Long> = mutableSetOf()) =
        this.filter { c -> seenIds.add(c.value.show.id) }
            .filter { c -> c.value.show.posterUrl.isNullOrBlank().not() }
            .filter { !hideLibraryItems.value || !it.value.show.favorite }

}

sealed interface Content {
    data class CMovie(val movie: Movie): Content
    data class CShow(val show: TVShow): Content
}

@Stable
@Immutable
data class MovieDiscoverState(
    val tvGenres: ImmutableList<Genre> = persistentListOf(),
    val movieGenres: ImmutableList<Genre> = persistentListOf(),
    val dialog: MovieDiscoverScreenModel.Dialog? = null,
    val genreWithContent: ImmutableList<Pair<Genre, ImmutableList<StateFlow<Content>>>> = persistentListOf(),
    val selectedGenre: Genre? = null,
    val selectedResource: Resource? = null,
) {
    val genres: ImmutableList<Genre>
        get() = (tvGenres + movieGenres).toImmutableList()
}
