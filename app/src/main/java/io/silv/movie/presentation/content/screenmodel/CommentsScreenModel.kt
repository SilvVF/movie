package io.silv.movie.presentation.content.screenmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.data.user.model.comment.CommentWithUser
import io.silv.movie.data.user.model.comment.PagedComment
import io.silv.movie.data.user.model.comment.ReplyWithUser
import io.silv.movie.data.user.repository.CommentPagingSource
import io.silv.movie.data.user.repository.CommentsRepository
import io.silv.movie.presentation.list.screenmodel.uniqueBy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.minutes


data class CommentsState(
    val messageCount: Long? = null,
    val recentMessage: CommentWithUser? = null,
    val sending: Boolean = false,
    val error: Boolean = false,
    val replyingTo: PagedComment? = null,
    val viewingReplies: PagedComment? = null
)

enum class CommentsPagedType { Newest, Top }

class CommentsScreenModel(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val commentsRepository: CommentsRepository,
    val contentId: Long,
    val isMovie: Boolean,
): StateScreenModel<CommentsState>(CommentsState()) {

    private val movieId: Long
        get() = contentId.takeIf { isMovie } ?: -1L
    private val showId: Long
        get() = contentId.takeIf { !isMovie } ?: -1L

    private val commentRefreshTrigger = Channel<Unit>()

    var sortMode by mutableStateOf(CommentsPagedType.Newest)
        private set

    var comment by mutableStateOf("")
        private set

    private val likedComments = mutableStateMapOf<Long, Boolean>()

    val likedCommentsImmutable by derivedStateOf { likedComments.toImmutableMap() }

    @OptIn(ObsoleteCoroutinesApi::class)
    private val refreshInterval = ticker(
        delayMillis = 1.minutes.inWholeMilliseconds,
        initialDelayMillis = 0L,
        context = screenModelScope.coroutineContext,
        mode = TickerMode.FIXED_PERIOD
    )

    init {
        refreshInterval.consumeAsFlow()
            .onEach {
                commentsRepository.getMostRecentComment(movieId, showId)
                    .onSuccess { (count, msg) ->
                        mutableState.update { state ->
                            state.copy(messageCount = count, recentMessage = msg)
                        }
                    }
                    .onFailure { Timber.e(it) }
            }
            .launchIn(screenModelScope)
    }

    val replies: StateFlow<ImmutableList<ReplyWithUser>> = state.map { it.viewingReplies }
        .filterNotNull()
        .distinctUntilChanged()
        .map { comment ->
            val result = commentsRepository.getRepliesForComment(comment.id)
                .onFailure { Timber.d(it) }
                .getOrDefault(emptyList())

            result.toImmutableList()
        }
        .stateIn(
            ioCoroutineScope,
            SharingStarted.WhileSubscribed(5_000),
            persistentListOf()
        )


    val pagingData = snapshotFlow { sortMode }
        .combine(
            commentRefreshTrigger.receiveAsFlow().onStart { emit(Unit) }
        ) { a, b -> a }
        .flatMapLatest { pagedType ->
            Pager(
                config = PagingConfig(30)
            ) {
                likedComments.clear()
                CommentPagingSource(
                    postgrest,
                    pagedType,
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
            val result = commentsRepository.likeComment(commentId).getOrNull() ?: return@launch

            if (likedComments.containsKey(result.cid)) {
                likedComments.remove(result.cid)
            } else {
                likedComments[result.cid] = true
            }
        }
    }

    fun unlikeComment(commentId: Long) {
        screenModelScope.launch {
            val result = commentsRepository.unlikeComment(commentId).getOrNull() ?: return@launch

            if (likedComments.containsKey(result.cid)) {
                likedComments.remove(result.cid)
            } else {
                likedComments[result.cid] = false
            }
        }
    }

    fun sendMessage(text: String) {
        screenModelScope.launch {
            mutableState.update { state -> state.copy(sending = true) }

            commentsRepository.sendComment(text, movieId, showId)
                .onSuccess {
                    comment = ""
                    commentRefreshTrigger.trySend(Unit)
                    withContext(Dispatchers.Main) {
                        mutableState.update { state -> state.copy(error = false) }
                    }
                }
                .onFailure {
                    mutableState.update { state -> state.copy(error = true) }
                }
            mutableState.update { state -> state.copy(sending = false) }
        }
    }

    fun sendReply(text: String, pagedComment: PagedComment) {
        screenModelScope.launch {

            commentsRepository.sendReply(pagedComment.id, text)
                .onSuccess {
                    comment = ""
                    commentRefreshTrigger.trySend(Unit)
                    mutableState.update { state ->
                        state.copy(error = false, replyingTo = null)
                    }
                }
                .onFailure {
                    mutableState.update { state -> state.copy(error = true) }
                }

            mutableState.update { state -> state.copy(sending = false) }
        }
    }

    fun updateViewing(comment: PagedComment?) {
        screenModelScope.launch {
            mutableState.update { it.copy(viewingReplies = comment) }
        }
    }

    fun updateReplyingTo(comment: PagedComment?) {
        screenModelScope.launch {
            mutableState.update { it.copy(replyingTo = comment) }
        }
    }

    fun updateComment(text: String) {
        comment = text
    }

    fun updateSortMode(type: CommentsPagedType) {
        sortMode = type
    }
}