package io.silv.movie.data.movie

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.silv.movie.core.SMovie
import io.silv.movie.data.movie.interactor.MoviesPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class SourceMoviePagingSource : PagingSource<Long, SMovie>() {

    abstract suspend fun getNextPage(page: Int): MoviesPage

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, SMovie> {
        val page = params.key ?: 1

        val moviesPage = try {
            withContext(Dispatchers.IO) {
                getNextPage(page.toInt())
                    .takeIf { it.movies.isNotEmpty() }
                    ?: error("Empty page")
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }

        return LoadResult.Page(
            data = moviesPage.movies,
            prevKey = null,
            nextKey = if (moviesPage.hasNextPage) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Long, SMovie>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}