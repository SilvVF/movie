package io.silv.movie.presentation.screenmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.data.content.lists.ListWithPostersRpcResponse
import io.silv.movie.data.content.lists.ContentListRepository
import io.silv.movie.data.content.lists.toListPreviewItem
import io.silv.movie.data.content.movie.local.LocalContentDelegate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable

class ListSearchPagingSource(
    private val postgrest: Postgrest,
    private val query: String,
): PagingSource<Int, ListWithPostersRpcResponse>() {

    @Serializable
    data class Params(
        val query: String,
        val off: Int,
        val lim: Int
    )

    override fun getRefreshKey(state: PagingState<Int, ListWithPostersRpcResponse>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListWithPostersRpcResponse> {

        return try {
            val offset = (params.key ?: 0) * params.loadSize
            val limit = params.loadSize


            val result = postgrest.rpc(
                "select_lists_with_poster_items_for_query",
                Params("%$query%", offset, limit)
            )
                .decodeList<ListWithPostersRpcResponse>()

            val nextStart = offset + limit

            LoadResult.Page(
                data = result,
                prevKey = params.key?.minus(1),
                nextKey = (params.key ?: 0).plus(1).takeIf {
                    nextStart <= (result.first().total ?: Long.MAX_VALUE) && result.size >= params.loadSize
                }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

}

fun <T : Any, V> PagingData<T>.uniqueBy(
    transform: (T) -> V
): PagingData<T> {
    val set = mutableSetOf<V>()
    return filter { data ->
        set.add(transform(data))
    }
}

class SearchForListScreenModel(
    private val postgrest: Postgrest,
    private val contentListRepository: ContentListRepository,
    private val local: LocalContentDelegate,
): ScreenModel {

    var query by mutableStateOf("")

    val state = snapshotFlow { query }
        .debounce(1000L)
        .filter { it.isNotBlank() }
        .flatMapLatest {
            Pager(
                config = PagingConfig(pageSize = 30)
            ) {
                ListSearchPagingSource(postgrest, it)
            }
                .flow.map { pagingData ->
                    pagingData.uniqueBy { it.listId }.map {
                        it.toListPreviewItem(contentListRepository, local, ioCoroutineScope)
                    }
                }
                .cachedIn(ioCoroutineScope)
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )
}