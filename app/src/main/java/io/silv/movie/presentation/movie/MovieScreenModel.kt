package io.silv.movie.presentation.movie

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.StateScreenModel
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.data.GetMovie
import io.silv.data.GetRemoteMovie
import io.silv.data.MoviePagedType
import io.silv.data.NetworkToLocalMovie
import io.silv.data.toDomain
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MovieScreenModel(
    private val getRemoteMovie: GetRemoteMovie,
    private val networkToLocalMovie: NetworkToLocalMovie,
    private val getMovie: GetMovie
): StateScreenModel<MovieState>(MovieState()) {


    val moviePagerFlowFlow = state.map { it.listing }
        .distinctUntilChanged()
        .map { listing ->
            Pager(
                PagingConfig(pageSize = 25)
            ) {
                getRemoteMovie.subscribe(listing)
            }.flow.map { pagingData ->
                pagingData.map { sMovie ->
                    networkToLocalMovie.await(sMovie.toDomain())
                        .let { localMovie -> getMovie.subscribe(localMovie.id) }
                        .stateIn(ioCoroutineScope)
                }
                    .filter { !it.value.favorite }
            }
                .cachedIn(ioCoroutineScope)
        }
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, emptyFlow())
}

@Immutable
@Stable
data class MovieState(
    val listing: MoviePagedType = MoviePagedType.Default.Popular
)