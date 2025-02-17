package io.silv.movie.presentation.screenmodel

import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.movie.data.content.movie.local.CreditRepository
import io.silv.movie.data.content.movie.network.SourceCreditsRepository
import io.silv.movie.data.content.movie.model.toDomain
import io.silv.movie.data.content.movie.model.Movie
import io.silv.movie.data.content.movie.local.MovieRepository
import io.silv.movie.data.content.movie.local.TrailerRepository
import io.silv.movie.data.content.movie.local.awaitUpdateFromSource
import io.silv.movie.data.content.movie.local.networkToLocalCredit
import io.silv.movie.data.content.movie.local.networkToLocalMovie
import io.silv.movie.data.content.movie.network.SourceMovieRepository
import io.silv.movie.data.content.movie.network.SourceTrailerRepository
import io.silv.movie.data.content.movie.model.Trailer
import io.silv.movie.data.content.movie.network.networkToLocalTrailer
import io.silv.movie.presentation.covers.cache.MovieCoverCache
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
import kotlinx.coroutines.supervisorScope
import timber.log.Timber

class MovieViewScreenModel(
    private val trailerSource: SourceTrailerRepository,
    private val trailerRepo: TrailerRepository,
    private val creditsSource: SourceCreditsRepository,
    private val creditsRepo: CreditRepository,
    private val movieSource: SourceMovieRepository,
    private val movieRepo: MovieRepository,
    private val movieCoverCache: MovieCoverCache,
    private val movieId: Long
) : StateScreenModel<MovieDetailsState>(MovieDetailsState.Loading) {

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
                val movie = movieRepo.getMovieById(movieId)

                when {
                    movie == null -> {
                        val smovie = movieSource.getMovie(movieId)

                        mutableState.value = if (smovie != null) {
                            MovieDetailsState.Success(
                                movie = movieRepo.networkToLocalMovie(smovie.toDomain())
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
                mutableState.value = MovieDetailsState.Error
            }
        }

        screenModelScope.launch {
            state.map { it.success?.movie?.id }
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest { movieId ->

                    val trailers = runCatching {
                        trailerRepo.getByMovieId(movieId)
                    }
                        .getOrDefault(emptyList())

                    if (trailers.isEmpty()) {
                        refreshMovieTrailers()
                    } else {
                        mutableState.updateSuccess { state ->
                            state.copy(trailers = trailers)
                        }
                    }
                }
        }

        movieRepo.observeMovieByIdOrNull(movieId).filterNotNull().onEach { new ->
            mutableState.updateSuccess {
                it.copy(movie = new)
            }
        }
            .launchIn(screenModelScope)
    }

    val credits = Pager(
        config = PagingConfig(pageSize = 30),
        pagingSourceFactory = { creditsRepo.movieCreditsPagingSource(movieId) },
    ).flow
        .cachedIn(screenModelScope)
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )


    private suspend fun refreshMovieCredits() {
        runCatching { creditsSource.awaitMovie(movieId) }
            .onSuccess { credits ->
                val movie = state.value.success?.movie
                for (sCredit in credits) {
                    creditsRepo.networkToLocalCredit(
                        sCredit.toDomain()
                            .copy(posterPath = movie?.posterUrl, title = movie?.title.orEmpty()),
                        movieId,
                        true
                    )
                }
            }
            .onFailure { Timber.e(it) }
    }

    private suspend fun refreshMovieTrailers() {

        val trailers = runCatching { trailerSource.awaitMovie(movieId) }.getOrDefault(emptyList())
            .map {
                trailerRepo.networkToLocalTrailer(
                    it.toDomain(), movieId, true
                )
            }

        mutableState.updateSuccess { state ->
            state.copy(trailers = trailers)
        }
    }

    private suspend fun refreshMovieInfo() {

        val smovie = runCatching { movieSource.getMovie(movieId) }.getOrNull()
        val movie = state.value.success?.movie

        if (smovie != null && movie != null) {
            movieRepo.awaitUpdateFromSource(movie, smovie, movieCoverCache)
        }
    }

    fun updateDialog(dialog: Dialog?) {
        screenModelScope.launch {
            mutableState.updateSuccess { state ->
                state.copy(dialog = dialog)
            }
        }
    }

    fun refresh() {
        screenModelScope.launch {

            mutableState.updateSuccess { it.copy(refreshing = true) }

            supervisorScope {
                listOf(
                    launch { refreshMovieInfo() },
                    launch { refreshMovieTrailers() },
                    launch { refreshMovieCredits() }
                )
                    .joinAll()
            }

            mutableState.updateSuccess { it.copy(refreshing = false) }
        }
    }

    @Stable
    sealed interface Dialog {

        @Stable
        data object Comments : Dialog

        @Stable
        data object FullCover : Dialog
    }
}


sealed class MovieDetailsState {

    @Stable
    data object Error : MovieDetailsState()

    @Stable
    data object Loading : MovieDetailsState()

    @Stable
    data class Success(
        val movie: Movie,
        val trailers: List<Trailer> = emptyList(),
        val refreshing: Boolean = false,
        val dialog: MovieViewScreenModel.Dialog? = null
    ) : MovieDetailsState()

    val success
        get() = this as? Success
}