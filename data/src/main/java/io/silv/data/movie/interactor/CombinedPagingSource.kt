package io.silv.data.movie.interactor

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class CombinedPagingSource<T: Any>(
    private val pagingSources: List<PagingSource<Long, T>>
): PagingSource<Long, T>() {

    override fun getRefreshKey(state: PagingState<Long, T>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, T> {
        val result = try {
            withContext(Dispatchers.IO) {
                pagingSources.map {
                    async { it.load(params) }
                }
                    .awaitAll()
            }
        } catch (e: Exception) {
            Log.d("MoviePagingSource", e.stackTraceToString())
            return LoadResult.Error(e)
        }

        return result
            .drop(1)
            .fold(result.first()) { acc, value ->
                when(value) {
                    is LoadResult.Error -> acc
                    is LoadResult.Invalid -> acc
                    is LoadResult.Page -> {
                        when (acc) {
                            is LoadResult.Error -> value
                            is LoadResult.Invalid -> value
                            is LoadResult.Page -> {
                                LoadResult.Page(
                                        data = acc.data + value.data,
                                        prevKey = null,
                                        nextKey = value.nextKey ?: acc.nextKey
                                )
                            }
                        }
                    }
            }
        }
    }
}