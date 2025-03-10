package io.silv.movie.presentation.screenmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.koin.koinScreenModel
import io.silv.core_ui.voyager.ContentScreen
import io.silv.core_ui.voyager.ioCoroutineScope
import io.silv.movie.data.supabase.CommentRepository
import io.silv.movie.data.supabase.ContentType
import io.silv.movie.data.supabase.UserRepository
import io.silv.movie.data.supabase.model.comment.CommentWithUser
import io.silv.movie.data.supabase.model.comment.PagedComment
import io.silv.movie.data.supabase.model.comment.ReplyWithUser
import io.silv.movie.presentation.EventProducer
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import kotlin.time.Duration.Companion.minutes

@Immutable
@Stable
data class CommentsState(
    val messageCount: Long? = null,
    val recentMessage: CommentWithUser? = null,
    val sending: Boolean = false,
    val sendError: SendError = SendError.None,
    val replyingTo: PagedComment? = null,
)

sealed interface RepliesState {
    data object Idle: RepliesState
    data object Loading: RepliesState
    data class Error(val reason: String): RepliesState
    data class Success(val data: List<ReplyWithUser>): RepliesState
}

enum class CommentsPagedType { Newest, Top }

sealed interface SendError {
    data object NotSignedIn: SendError
    data class Failed(val message: String): SendError
    data object None: SendError
}

sealed interface CommentEvent {
    data object SentMessage: CommentEvent
    data object SortChanged: CommentEvent
}


@Composable
fun ContentScreen.getCommentsScreenModel() = koinScreenModel<CommentsScreenModel> { parametersOf(this.id, this.isMovie) }

class CommentsScreenModel(
    private val userRepository: UserRepository,
    private val commentsRepository: CommentRepository,
    val contentId: Long,
    val isMovie: Boolean,
):
    StateScreenModel<CommentsState>(CommentsState()),
    EventProducer<CommentEvent> by EventProducer.default() {

    private val movieId: Long
        get() = contentId.takeIf { isMovie } ?: -1L
    private val showId: Long
        get() = contentId.takeIf { !isMovie } ?: -1L

    var sortMode by mutableStateOf(CommentsPagedType.Newest)
        private set

    var comment by mutableStateOf(TextFieldValue(""))
        private set

    val likedComments = mutableStateMapOf<Long, Boolean>()

    val repliesForComment = mutableStateMapOf<Long, RepliesState>()

    @OptIn(ObsoleteCoroutinesApi::class)
    private val refreshInterval = ticker(
        delayMillis = 1.minutes.inWholeMilliseconds,
        initialDelayMillis = 0L,
        context = screenModelScope.coroutineContext,
        mode = TickerMode.FIXED_PERIOD
    )

    private val recentManualRefresh = Channel<Unit>()

    init {
        refreshInterval.consumeAsFlow()
            .combine(recentManualRefresh.receiveAsFlow().onStart { emit(Unit) }) { _, _-> }
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

    private var pagingSource:  PagingSource<Int, PagedComment>? = null

    val pagingData = snapshotFlow { sortMode }
        .flatMapLatest { pagedType ->
            Pager(
                config = PagingConfig(30)
            ) {
                likedComments.clear()
                commentsRepository.pagingSource(pagedType, contentId, if (isMovie) ContentType.Movie else ContentType.Show)
                    .also { pagingSource = it }
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
            val result = commentsRepository.likeComment(commentId)
                .onFailure {
                    mutableState.update {state ->
                        state.copy(sendError = SendError.Failed(it.localizedMessage.orEmpty()))
                    }
                }
                .getOrNull() ?: return@launch

            if (likedComments.containsKey(result.cid)) {
                likedComments.remove(result.cid)
            } else {
                likedComments[result.cid] = true
            }
        }
    }

    fun unlikeComment(commentId: Long) {
        screenModelScope.launch {
            val result = commentsRepository.unlikeComment(commentId)
                .onFailure {  }
                .getOrNull() ?: return@launch

            if (likedComments.containsKey(result.cid)) {
                likedComments.remove(result.cid)
            } else {
                likedComments[result.cid] = false
            }
        }
    }

    fun fetchReplies(comment: PagedComment) {
        val commentId = comment.id
        repliesForComment[commentId] = RepliesState.Loading
        screenModelScope.launch {
            repliesForComment[commentId] = commentsRepository.getRepliesForComment(commentId)
                .fold(
                    onFailure = {
                        RepliesState.Error(it.localizedMessage ?: "error")
                    },
                    onSuccess = {
                        RepliesState.Success(it)
                    }
                )
        }
    }

    fun sendMessage(text: String) {

        state.value.replyingTo?.let {
            return sendReply(text, it)
        }

        screenModelScope.launch {
            if (userRepository.currentUser.value == null) {
                mutableState.update { state -> state.copy(sendError = SendError.NotSignedIn) }
                return@launch
            }
            mutableState.update { state -> state.copy(sending = true) }

            commentsRepository.sendComment(text, movieId, showId)
                .onSuccess {
                    comment = comment.copy("", selection = TextRange(1))
                    pagingSource?.invalidate()
                    mutableState.update { state -> state.copy(sendError = SendError.None) }
                    emitEvent(CommentEvent.SentMessage)
                }
                .onFailure {
                    mutableState.update { state ->
                        state.copy(sendError = SendError.Failed(it.localizedMessage.orEmpty()))
                    }
                }
            mutableState.update { state -> state.copy(sending = false) }
        }
    }

    private fun sendReply(text: String, pagedComment: PagedComment) {
        screenModelScope.launch {
            if (userRepository.currentUser.value == null) {
                mutableState.update { state -> state.copy(sendError = SendError.NotSignedIn) }
                return@launch
            }
            commentsRepository.sendReply(pagedComment.id, text)
                .onSuccess {
                    comment = comment.copy("", selection = TextRange(1))
                    fetchReplies(pagedComment)

                    mutableState.update { state ->
                        state.copy(
                            sendError = SendError.None,
                            replyingTo = null
                        )
                    }
                }
                .onFailure {
                    mutableState.update { state ->
                        state.copy(sendError = SendError.Failed(it.localizedMessage.orEmpty()))
                    }
                }

            mutableState.update { state -> state.copy(sending = false) }
        }
    }

    fun updateReplyingTo(comment: PagedComment?) {
        screenModelScope.launch {
            Timber.d(comment.toString())
            mutableState.update { it.copy(replyingTo = comment) }
        }
    }

    fun updateComment(text: TextFieldValue) {
        comment = text
    }

    fun updateSortMode(type: CommentsPagedType) {
        sortMode = type
        tryEmitEvent(CommentEvent.SortChanged)
    }
}