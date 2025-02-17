package io.silv.movie.presentation.screenmodel

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.map
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.data.model.ListWithPostersRpcResponse
import io.silv.movie.data.local.ContentListRepository
import io.silv.movie.data.model.toListPreviewItem
import io.silv.movie.data.local.LocalContentDelegate
import io.silv.movie.data.supabase.SupabaseConstants
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

enum class ListPagedType(val v: Int) {
    Recent(1),
    MoreFromSubscribed(2),
    Popular(3)
}

class ListPagedScreenModel(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val pagedType: ListPagedType,
    private val contentListRepository: ContentListRepository,
    private val local: LocalContentDelegate,
): ScreenModel {

    private inner class ListPagingSource: PagingSource<Int, ListWithPostersRpcResponse>() {

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

                val rpc = when (pagedType) {
                    ListPagedType.MoreFromSubscribed -> postgrest.rpc(
                        "select_recommended_by_subscriptions",
                        SupabaseConstants.SelectMoreFromSubscribedParams(uid = auth.currentUserOrNull()?.id!!, off = offset,lim = limit)
                    )
                    ListPagedType.Popular -> postgrest.rpc(
                        "select_most_popular_lists_with_poster_items",
                        PopularListParams(lim = limit, off = offset)
                    )
                    ListPagedType.Recent -> postgrest.rpc(
                        "select_most_recent_lists_with_poster_items",
                        PopularListParams(lim = limit, off = offset)
                    )
                }

                val result = rpc.decodeList<ListWithPostersRpcResponse>()

                val nextStart = offset + limit

                LoadResult.Page(
                    data = result,
                    prevKey = params.key?.minus(1),
                    nextKey = (params.key ?: 0).plus(1).takeIf {
                        nextStart <= (result.first().total ?: Long.MAX_VALUE) && result.size >= params.loadSize
                    }
                )
            } catch (e: Exception) {
                Timber.d(e)
                LoadResult.Error(e)
            }
        }
    }

    val pagingData =  Pager(
        config = PagingConfig(pageSize = 30)
    ) {
        ListPagingSource()
    }
        .flow.map { pagingData ->
            pagingData.uniqueBy { it.listId }.map {
                it.toListPreviewItem(contentListRepository, local, ioCoroutineScope)
            }
        }
        .cachedIn(ioCoroutineScope)
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PagingData.empty()
        )
}
