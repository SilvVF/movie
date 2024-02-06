package io.silv.movie.data

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.silv.movie.domain.movie.MoviePagedType
import io.silv.movie.types.movie.popular.MovieListResponse

class MoviePagingSource(
    private val movieApi: MovieApi,
    private val moviePagedType: MoviePagedType,
): PagingSource<Int, MovieListResponse.Result>() {
    override fun getRefreshKey(state: PagingState<Int, MovieListResponse.Result>): Int? {
        return state.anchorPosition?.let {
            state.closestPageToPosition(it)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(it)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MovieListResponse.Result> {
        return try {

            Log.d("Paging", params.key.toString())

            val pageNumber = params.key ?: 1
            val response = when(moviePagedType) {
                MoviePagedType.Popular -> movieApi.getPopularMovies(pageNumber)
                MoviePagedType.TopRated -> movieApi.getTopRatedMovies(pageNumber)
                MoviePagedType.Upcoming -> movieApi.getUpcomingMovies(pageNumber)
                is MoviePagedType.Filter -> movieApi.getSearchMovies(pageNumber, moviePagedType.searchQuery)
            }

            LoadResult.Page(
                data = response.results,
                prevKey = params.key?.minus(1),
                nextKey = pageNumber.plus(1).takeIf { it <= response.totalPages }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            LoadResult.Error(e)
        }
    }
}