package io.silv.data.tv

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.silv.core.STVShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class SourceTVPagingSource : PagingSource<Long, STVShow>() {


    abstract suspend fun getNextPage(page: Int): TVPage

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, STVShow> {
        val page = params.key ?: 1

        val moviesPage = try {
            withContext(Dispatchers.IO) {
                getNextPage(page.toInt())
                    .takeIf { it.shows.isNotEmpty() }
                    ?: error("Empty page")
            }
        } catch (e: Exception) {
            Log.d("MoviePagingSource", e.stackTraceToString())
            return LoadResult.Error(e)
        }

        return LoadResult.Page(
            data = moviesPage.shows,
            prevKey = null,
            nextKey = if (moviesPage.hasNextPage) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Long, STVShow>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }
}
