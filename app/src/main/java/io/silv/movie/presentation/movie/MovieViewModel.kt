package io.silv.movie.presentation.movie

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import io.silv.movie.domain.movie.GetPagingSource
import io.silv.movie.domain.movie.MoviePagedType
import io.silv.movie.types.movie.popular.MovieListResponse
import io.silv.movie.ui.components.Poster
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MovieViewModel(
    private val getPagingSource: GetPagingSource,
): ViewModel() {

    private val moviePagedType = MutableStateFlow<MoviePagedType>(MoviePagedType.Popular)
    val selectedPageType = moviePagedType.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val moviePagedData = moviePagedType.flatMapLatest {
        getPagingSource(PagingConfig(pageSize = 50), it)
        .flow
        .map { value: PagingData<MovieListResponse.Result> ->
            val seen = mutableSetOf<Int>()
            value.map {
                Poster(
                    id = it.id,
                    title = it.title,
                    url = "https://image.tmdb.org/t/p/original/${it.posterPath}"
                )
            }
                .filter { seen.add(it.id) }
                .filter { it.url.takeLast(4) != "null" }
        }
            .cachedIn(viewModelScope)
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )

    fun changePagingData(pagedType: MoviePagedType) {
        viewModelScope.launch {
            moviePagedType.emit(pagedType)
        }
    }
}

sealed class MoviePagingData(
    val pagingData: PagingData<Poster>,
    val title: String,
    val icon: ImageVector,
    val pagedType: MoviePagedType
) {
    data class Popular(val data: PagingData<Poster>)
        : MoviePagingData(data, "Popular", Icons.Filled.AutoAwesome, MoviePagedType.Popular)

    data class TopRated(val data: PagingData<Poster>) :
        MoviePagingData(data, "Top Rated", Icons.Filled.Whatshot, MoviePagedType.TopRated)

    data class Upcoming(val data: PagingData<Poster>) :
        MoviePagingData(data, "Upcoming", Icons.Filled.NewReleases, MoviePagedType.Upcoming)
}