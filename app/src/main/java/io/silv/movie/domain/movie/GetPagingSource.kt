package io.silv.movie.domain.movie

import androidx.paging.Pager
import androidx.paging.PagingConfig
import io.silv.movie.data.MovieApi
import io.silv.movie.data.MoviePagingSource
import io.silv.movie.types.movie.popular.MovieListResponse

class GetPagingSource(
    private val movieApi: MovieApi,
) {

    operator fun invoke(
        config: PagingConfig,
        moviePagedType: MoviePagedType
    ): Pager<Int, MovieListResponse.Result> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                MoviePagingSource(
                    movieApi = movieApi,
                    moviePagedType
                )
            }
        )
    }
}


sealed interface MoviePagedType {
    data object Popular: MoviePagedType
    data object TopRated: MoviePagedType
    data object Upcoming: MoviePagedType
    data class Filter(val searchQuery: String): MoviePagedType
}