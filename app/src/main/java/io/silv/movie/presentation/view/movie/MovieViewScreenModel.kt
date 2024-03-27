package io.silv.movie.presentation.view.movie

import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.cache.MovieCoverCache
import io.silv.movie.data.credits.CreditRepository
import io.silv.movie.data.credits.GetRemoteCredits
import io.silv.movie.data.credits.NetworkToLocalCredit
import io.silv.movie.data.credits.toDomain
import io.silv.movie.data.movie.interactor.GetMovie
import io.silv.movie.data.movie.interactor.GetRemoteMovie
import io.silv.movie.data.movie.interactor.NetworkToLocalMovie
import io.silv.movie.data.movie.interactor.UpdateMovie
import io.silv.movie.data.movie.model.Movie
import io.silv.movie.data.movie.model.toDomain
import io.silv.movie.data.movie.model.toMovieUpdate
import io.silv.movie.data.trailers.GetMovieTrailers
import io.silv.movie.data.trailers.GetRemoteTrailers
import io.silv.movie.data.trailers.NetworkToLocalTrailer
import io.silv.movie.data.trailers.Trailer
import io.silv.movie.data.trailers.toDomain
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import timber.log.Timber

class MovieViewScreenModel(
    private val getMovieTrailers: GetMovieTrailers,
    private val getRemoteTrailers: GetRemoteTrailers,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val networkToLocalTrailer: NetworkToLocalTrailer,
    private val networkToLocalCredit: NetworkToLocalCredit,
    private val getRemoteCredits: GetRemoteCredits,
    private val creditsRepository: CreditRepository,
    private val getRemoteMovie: GetRemoteMovie,
    private val getMovie: GetMovie,
    private val updateMovie: UpdateMovie,
    private val movieCoverCache: MovieCoverCache,
    private val movieId: Long
): StateScreenModel<MovieDetailsState>(MovieDetailsState.Loading) {

    private fun MutableStateFlow<MovieDetailsState>.updateSuccess(
        function: (MovieDetailsState.Success) -> MovieDetailsState.Success
    ) {
        update {
            when (it) {
                is MovieDetailsState.Success -> function(it)
                else -> it
            }
        }
    }

    init {
        screenModelScope.launch {
            try {
                val movie = getMovie.await(id = movieId)

                when {
                    movie == null -> {
                        val smovie = getRemoteMovie.awaitOne(movieId)

                        mutableState.value = if (smovie != null) {
                            MovieDetailsState.Success(
                                movie = networkToLocalMovie.await(smovie.toDomain())
                            )
                        } else {
                            MovieDetailsState.Error
                        }
                    }

                    movie.needsInit -> {
                        mutableState.value =
                            MovieDetailsState.Success(movie = movie)
                        refreshMovieInfo()
                    }

                    else -> {
                        mutableState.value =
                            MovieDetailsState.Success(movie = movie)
                    }
                }
                refreshMovieCredits()
            } catch (e: Exception) {
                mutableState.value =  MovieDetailsState.Error
            }
        }

        screenModelScope.launch {
            state.map { it.success?.movie?.id }
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { movieId ->

                    val trailers = runCatching{  getMovieTrailers.await(movieId) }.getOrDefault(emptyList())

                    if (trailers.isEmpty()) {
                        refreshMovieTrailers()
                    } else {
                        mutableState.updateSuccess { state ->
                            state.copy(
                                trailers = trailers.toImmutableList()
                            )
                        }
                    }
                }
        }

        getMovie.subscribeOrNull(movieId).filterNotNull().onEach { new ->
            mutableState.updateSuccess {
                it.copy(movie = new)
            }
        }
            .launchIn(screenModelScope)
    }

    val credits = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { creditsRepository.movieCreditsPagingSource(movieId) },
    ).flow
        .cachedIn(screenModelScope)
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )


    private suspend fun refreshMovieCredits() {
        runCatching { getRemoteCredits.awaitMovie(movieId) }
            .onSuccess { credits ->
                val movie = state.value.success?.movie
                for (sCredit in credits) {
                    networkToLocalCredit.await(
                        sCredit.toDomain().copy(posterPath = movie?.posterUrl, title = movie?.title.orEmpty()),
                        movieId,
                        true
                    )
                }
            }
            .onFailure { Timber.e(it) }
    }

    private suspend fun refreshMovieTrailers() {

        val trailers = runCatching { getRemoteTrailers.awaitMovie(movieId) }.getOrDefault(emptyList())
            .map {
                networkToLocalTrailer.await(
                    it.toDomain(), movieId, true
                )
            }

        mutableState.updateSuccess {state ->
            state.copy(
                trailers = trailers.toImmutableList()
            )
        }
    }

    private suspend fun refreshMovieInfo() {

        val smovie = runCatching { getRemoteMovie.awaitOne(movieId) }.getOrNull()
        val movie = state.value.success?.movie

        if (smovie != null && movie != null) {
            updateMovie.awaitUpdateFromSource(movie, smovie, movieCoverCache)
        }
    }

    fun updateDialog(dialog: Dialog?) {
        screenModelScope.launch {
            mutableState.updateSuccess {state ->
                state.copy(dialog = dialog)
            }
        }
    }

    fun toggleMovieFavorite(m: Movie) {
        screenModelScope.launch {
            val movie = getMovie.await(m.id) ?: return@launch

            val new = movie.copy(favorite = !movie.favorite)

            if(!new.favorite) {
                movieCoverCache.deleteFromCache(movie)
            }
            updateMovie.await(new.toMovieUpdate())
        }
    }

    fun refresh() {
        screenModelScope.launch {

            mutableState.updateSuccess { it.copy(refreshing = true) }

            listOf(
                launch { refreshMovieInfo() },
                launch { refreshMovieTrailers() },
                launch { refreshMovieCredits() }
            )
                .joinAll()

            mutableState.updateSuccess { it.copy(refreshing = false) }
        }
    }

    @Stable
    sealed interface Dialog {

        @Stable
        data object FullCover: Dialog
    }
}


sealed class MovieDetailsState {

    @Stable
    data object Error: MovieDetailsState()

    @Stable
    data object Loading: MovieDetailsState()

    @Stable
    data class Success(
        val movie: Movie,
        val trailers: ImmutableList<Trailer> = persistentListOf(),
        val refreshing: Boolean = false,
        val dialog: MovieViewScreenModel.Dialog? = null
    ): MovieDetailsState()

    val success
        get() = this as? Success
}