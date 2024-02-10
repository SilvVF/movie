package io.silv.data.movie.interactor

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class CombinedPagingSource<T: Any>(
    private val pagingSources: List<PagingSource<Long, T>>
): PagingSource<Long, T>() {

    private val hasNext = MutableList(pagingSources.size) { true  }

    override fun getRefreshKey(state: PagingState<Long, T>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, T> {
        val result = try {
            withContext(Dispatchers.IO) {
                pagingSources.mapIndexed { i, source ->
                    if (hasNext[i]) {
                        async { source.load(params) }
                    } else {
                        null
                    }
                }
                    .filterNotNull()
                    .awaitAll()
            }
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }

        return result
            .drop(1)
            .foldIndexed(result.first()) { i,  acc, value ->
                when(value) {
                    is LoadResult.Error -> acc
                    is LoadResult.Invalid -> acc
                    is LoadResult.Page -> {

                        hasNext[i] = value.nextKey != null

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