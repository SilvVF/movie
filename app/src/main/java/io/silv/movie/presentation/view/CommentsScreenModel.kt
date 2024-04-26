package io.silv.movie.presentation.view

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.presentation.browse.lists.uniqueBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import kotlin.time.Duration.Companion.minutes

@Serializable
data class Comment(
    val id: Long,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("user_id")
    val userId: String,
    val message: String,
    @SerialName("movie_id")
    val movieId: Long,
    @SerialName("show_id")
    val showId: Long,
    val likes: Long
)

@Serializable
data class PagedComment(
    val id: Long,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("user_id")
    val userId: String,
    val message: String,
    @SerialName("movie_id")
    val movieId: Long,
    @SerialName("show_id")
    val showId: Long,
    @SerialName("profile_image")
    val profileImage: String,
    val username: String,
    val likes: Long,
    @SerialName("user_liked")
    val userLiked: Boolean,
    val total: Long,
)


@Serializable
data class CommentWithUser(
    val id: Long,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("user_id")
    val userId: String,
    val message: String,
    val users: Users,
) {

    @Serializable
    data class Users(
        val username: String,
        @SerialName("profile_image")
        val profileImage: String,
    )
}

data class CommentsState(
    val messageCount: Long? = null,
    val recentMessage: CommentWithUser? = null
)

enum class CommentsPagedType {
    Newest, Top
}


private class CommentsPagingSource(
    private val postgrest: Postgrest,
    private val pagedType: CommentsPagedType,
    private val movieId: Long,
    private val showId: Long,
    private val userId: String,
): PagingSource<Int, PagedComment>() {

    @Serializable
    data class Params(
        val uid: String,
        @SerialName("mid")
        private val movieId: Long,
        @SerialName("sid")
        private val showId: Long,
        val lim: Int,
        val off: Int
    )

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

            val result = postgrest.rpc(
                "select_comments_for_content_with_info",
                        Params(
                            uid = userId,
                            movieId = movieId,
                            showId = showId,
                            lim = limit,
                            off = offset
                        )
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

@Serializable
data class CLike(
    @SerialName("user_id")
    val userId: String,
    val cid: Long
)

class CommentsScreenModel(
    private val postgrest: Postgrest,
    private val auth: Auth,
    val contentId: Long,
    val isMovie: Boolean,
): StateScreenModel<CommentsState>(CommentsState()) {

    private val movieId: Long
        get() = contentId.takeIf { isMovie } ?: -1L
    private val showId: Long
        get() = contentId.takeIf { !isMovie } ?: -1L

    var sortMode by mutableStateOf(CommentsPagedType.Newest)
        private set

    val likedComments = mutableStateMapOf<Long, Boolean>()

    @OptIn(ObsoleteCoroutinesApi::class)
    private val refreshInterval = ticker(
        delayMillis = 1.minutes.inWholeMilliseconds,
        initialDelayMillis = 0L,
        context = screenModelScope.coroutineContext,
        mode = TickerMode.FIXED_PERIOD
    )


    init {
        screenModelScope.launch {
            refreshInterval.consumeAsFlow()
                .collectLatest {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val res = postgrest.from("comment")
                                .select(
                                    columns = Columns.raw(
                                        "id, created_at, user_id, message, users:users!comment_user_id_fkey(username, profile_image)"
                                    )
                                ) {
                                    count(Count.ESTIMATED)
                                    filter {
                                        eq("show_id", showId)
                                        eq("movie_id", movieId)
                                    }
                                    order("created_at", Order.DESCENDING)
                                    limit(1)
                                }

                            res.countOrNull() to res.decodeSingle<CommentWithUser>()
                        }
                            .onSuccess { (count, msg) ->
                                withContext(Dispatchers.Main) {
                                    mutableState.update {state ->
                                        state.copy(messageCount = count, recentMessage = msg)
                                    }
                                }
                            }
                            .onFailure {
                                Timber.e(it)
                            }
                    }
                }
        }
    }

    val pagingData = snapshotFlow { sortMode }
        .flatMapLatest { pagedType ->
            Pager(
                config = PagingConfig(30)
            ) {
                likedComments.clear()
                CommentsPagingSource(postgrest, pagedType,
                    movieId,
                    showId,
                    auth.currentUserOrNull()?.id ?: ""
                )
            }
                .flow.map { data ->
                    data.uniqueBy { it.id }
                }
                .cachedIn(ioCoroutineScope)
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5_000),
            initialValue = PagingData.empty()
        )

    fun likeComment(commentId: Long) {
        screenModelScope.launch {
            try {
                postgrest["clikes"]
                    .insert(
                        CLike(
                            userId = auth.currentUserOrNull()!!.id,
                            cid = commentId
                        )
                    )

                likedComments[commentId] = true
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun unlikeComment(commentId: Long) {
        screenModelScope.launch {
            try {
                postgrest["clikes"]
                    .delete {
                        filter {
                            eq("cid", commentId)
                            eq("user_id", auth.currentUserOrNull()!!.id)
                        }
                    }

                likedComments[commentId] = false
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun updateSortMode(type: CommentsPagedType) {
        sortMode = type
    }
}