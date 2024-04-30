package io.silv.movie.data.content.tv

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.silv.movie.data.content.tv.interactor.TVPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class SourceTVPagingSource : PagingSource<Long, io.silv.movie.core.STVShow>() {


    abstract suspend fun getNextPage(page: Int): TVPage

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, io.silv.movie.core.STVShow> {
        val page = params.key ?: 1

        val moviesPage = try {
            withContext(Dispatchers.IO) {
                getNextPage(page.toInt())
                    .takeIf { it.shows.isNotEmpty() }
                    ?: error("Empty page")
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }

        return LoadResult.Page(
            data = moviesPage.shows,
            prevKey = null,
            nextKey = if (moviesPage.hasNextPage) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Long, io.silv.movie.core.STVShow>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}
