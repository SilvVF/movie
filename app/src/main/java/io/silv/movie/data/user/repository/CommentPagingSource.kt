package io.silv.movie.data.user.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.jan.supabase.postgrest.Postgrest
import io.silv.movie.data.user.SupabaseConstants
import io.silv.movie.data.user.SupabaseConstants.RPC.selectCommentsForContent
import io.silv.movie.data.user.model.comment.PagedComment
import io.silv.movie.presentation.content.screenmodel.CommentsPagedType
import timber.log.Timber

class CommentPagingSource(
    private val postgrest: Postgrest,
    private val pagedType: CommentsPagedType,
    private val movieId: Long,
    private val showId: Long,
    private val userId: String,
): PagingSource<Int, PagedComment>() {

    override fun getRefreshKey(state: PagingState<Int, PagedComment>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey ?: anchorPage?.nextKey
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PagedComment> {

        return try {
            val offset = (params.key ?: 0) * params.loadSize
            val limit = params.loadSize

            val result = postgrest.selectCommentsForContent(
                userId, movieId, showId, offset, limit,
                order = when(pagedType) {
                    CommentsPagedType.Newest -> SupabaseConstants.CommentsOrder.Newest
                    CommentsPagedType.Top -> SupabaseConstants.CommentsOrder.Top
                }
            )
            Timber.d(result.data)
            val data = result.decodeList<PagedComment>()

            LoadResult.Page(
                data = data,
                prevKey = params.key?.minus(1),
                nextKey = (params.key ?: 0).plus(1).takeIf {
                    (offset + limit) <= (data.first().total) && data.size >= params.loadSize
                }
            )
        } catch (e: Exception) {
            Timber.d(e)
            LoadResult.Error(e)
        }
    }
}