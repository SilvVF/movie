package io.silv.movie.data.supabase

import androidx.paging.PagingSource
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.silv.movie.IoDispatcher
import io.silv.movie.core.NetworkMonitor
import io.silv.movie.core.filterUniqueBy
import io.silv.movie.core.suspendRunCatching
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.model.ListWithPostersRpcResponse
import io.silv.movie.data.model.Movie
import io.silv.movie.data.model.TVShow
import io.silv.movie.data.supabase.SupabaseConstants.CLIKES
import io.silv.movie.data.supabase.SupabaseConstants.COMMENT
import io.silv.movie.data.supabase.SupabaseConstants.FAVORITE_MOVIES
import io.silv.movie.data.supabase.SupabaseConstants.LIST_ITEM
import io.silv.movie.data.supabase.SupabaseConstants.REPLY
import io.silv.movie.data.supabase.SupabaseConstants.RPC.moreFromSubscribed
import io.silv.movie.data.supabase.SupabaseConstants.RPC.subscribedListWithItems
import io.silv.movie.data.supabase.SupabaseConstants.SUBSCRIPTION
import io.silv.movie.data.supabase.SupabaseConstants.USERS
import io.silv.movie.data.supabase.SupabaseConstants.USER_LIST
import io.silv.movie.data.supabase.model.FavoriteMovie
import io.silv.movie.data.supabase.model.FavoriteMovieInsert
import io.silv.movie.data.supabase.model.User
import io.silv.movie.data.supabase.model.comment.CLike
import io.silv.movie.data.supabase.model.comment.CommentWithUser
import io.silv.movie.data.supabase.model.comment.PagedComment
import io.silv.movie.data.supabase.model.comment.ReplyWithUser
import io.silv.movie.data.supabase.model.comment.SendComment
import io.silv.movie.data.supabase.model.comment.SendReply
import io.silv.movie.data.supabase.model.list.ListItem
import io.silv.movie.data.supabase.model.list.ListWithItems
import io.silv.movie.data.supabase.model.list.UserList
import io.silv.movie.data.supabase.model.list.UserListUpdate
import io.silv.movie.data.supabase.model.subscription.Subscription
import io.silv.movie.data.supabase.model.subscription.SubscriptionWithItem
import io.silv.movie.data.supabase.model.toListWithItems
import io.silv.movie.prefrences.BasePreferences
import io.silv.movie.presentation.screenmodel.CommentsPagedType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import timber.log.Timber
import java.util.UUID

interface UserRepository {
    val currentUser: StateFlow<User?>
    suspend fun deleteAccount(): Boolean
    suspend fun signOut(scope: SignOutScope = SignOutScope.LOCAL): Boolean
    suspend fun getUser(id: String): Result<User>
    suspend fun updateUser(user: User): Result<User>
    suspend fun registerWithEmailAndPassword(email: String, password: String): Boolean
    suspend fun signInWithEmailAndPassword(email: String, password: String): Boolean
    suspend fun resetPassword(email: String): Boolean
}

interface CommentRepository {
    suspend fun unlikeComment(commentId: Long): Result<CLike>
    suspend fun sendReply(commentId: Long, message: String): Result<Unit>
    suspend fun getMostRecentComment(
        movieId: Long = -1,
        showId: Long = -1
    ): Result<Pair<Long, CommentWithUser>>

    suspend fun sendComment(
        message: String,
        movieId: Long = -1,
        showId: Long = -1
    ): Result<Unit>

    suspend fun likeComment(commentId: Long): Result<CLike>
    suspend fun getRepliesForComment(commentId: Long): Result<List<ReplyWithUser>>
    fun pagingSource(
        pagedType: CommentsPagedType,
        contentId: Long,
        contentType: ContentType,
    ): PagingSource<Int, PagedComment>
}

interface ListRepository {
    suspend fun selectFavoritesList(userId: String): Result<List<FavoriteMovie>>
    suspend fun unsubscribeFromList(id: String): Result<Unit>
    suspend fun subscribeToList(id: String): Result<Unit>
    suspend fun addShowToFavorites(show: TVShow): Boolean
    suspend fun addMovieToFavoritesList(movie: Movie): Boolean
    suspend fun selectAllItemsForList(listId: String): Result<List<ListItem>>
    suspend fun selectListWithItemsById(listId: String): Result<ListWithItems>
    suspend fun selectListById(listId: String): Result<UserList>
    suspend fun selectRecommendedFromSubscriptions(): Result<List<ListWithPostersRpcResponse>>
    suspend fun selectSubscriptions(userId: String): Result<List<ListWithItems>>
    suspend fun selectListsByUserId(userId: String): Result<List<ListWithItems>>
    suspend fun deleteFromFavorites(id: Long, type: ContentType): Boolean
    suspend fun updateList(update: UserListUpdate): Boolean
    suspend fun deleteList(listId: String): Boolean
    suspend fun insertList(name: String): Result<UserList>
    suspend fun deleteFromList(id: Long, contentList: ContentList, type: ContentType): Boolean
    suspend fun addToList(
        id: Long,
        posterPath: String?,
        title: String,
        contentList: ContentList,
        type: ContentType
    ): Boolean
}

interface BackendRepository : UserRepository, ListRepository, CommentRepository

sealed interface BackendEvent {
    data class UserUpdated(val user: User) : BackendEvent
}

class BackendRepositoryImpl(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val networkMonitor: NetworkMonitor,
    basePreferences: BasePreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackendRepository {

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val savedUser = basePreferences.savedUser()

    private val events = Channel<BackendEvent>(30)

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser
        .map { user -> user ?: savedUser.get() }
        .flowOn(ioDispatcher)
        .stateIn(
            scope,
            SharingStarted.Lazily,
            null
        )


    init {
        scope.launch {
            supervisorScope {
                listenToAuthEvents()
                listenForUpdates()
            }
        }
    }

    private fun CoroutineScope.listenToAuthEvents(): Job {
        return auth.sessionStatus
            .combine(networkMonitor.isOnline, ::Pair)
            .filter { (_, online) -> online }
            .onEach { (status, online) ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val id = status.session.user?.id ?: return@onEach
                        getUser(id).onSuccess {
                            savedUser.set(it)
                        }
                    }

                    is SessionStatus.NotAuthenticated -> {
                        savedUser.set(null)
                    }

                    else -> Unit
                }
            }
            .catch { Timber.e(it) }
            .launchIn(this)
    }

    private fun CoroutineScope.listenForUpdates(): Job {
        return events.receiveAsFlow().onEach { event ->
            when (event) {
                is BackendEvent.UserUpdated -> launch {
                    _currentUser.emit(event.user)
                }
            }
        }
            .catch { Timber.e(it) }
            .launchIn(this)
    }


    override suspend fun deleteAccount(): Boolean {
        return suspendRunCatching {
            postgrest.rpc("deleteUser")
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    override suspend fun signOut(scope: SignOutScope): Boolean {
        return suspendRunCatching {
            auth.signOut(scope = scope)
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    override suspend fun getUser(id: String): Result<User> = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[TABLE_USERS].select {
                filter { eq("user_id", id) }
                limit(1)
                order(column = "user_id", Order.DESCENDING)
            }
                .decodeSingle<User>()
        }
    }

    override suspend fun updateUser(user: User): Result<User> = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[TABLE_USERS]
                .update(user) {
                    select()
                    filter { eq("user_id", user.userId) }
                    limit(count = 1)
                    order(column = "user_id", Order.DESCENDING)
                }
                .decodeSingle<User>()
        }
            .onSuccess {
                events.send(BackendEvent.UserUpdated(it))
            }
    }

    override suspend fun registerWithEmailAndPassword(email: String, password: String): Boolean =
        withContext(ioDispatcher) {
            suspendRunCatching {
                auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
            }
                .onFailure { Timber.e(it) }
                .isSuccess
        }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Boolean =
        withContext(ioDispatcher) {
            suspendRunCatching {
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }
                .onFailure { Timber.e(it) }
                .isSuccess
        }

    override suspend fun resetPassword(email: String): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            auth.resetPasswordForEmail(email)
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    override suspend fun selectFavoritesList(userId: String): Result<List<FavoriteMovie>> =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest[FAVORITE_MOVIES]
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<FavoriteMovie>()
            }
        }

    override suspend fun unsubscribeFromList(id: String): Result<Unit> = withContext(ioDispatcher) {
        suspendRunCatching<Unit> {
            postgrest[SUBSCRIPTION]
                .delete {
                    filter {
                        eq("list_id", id)
                        eq("user_id", auth.currentUserOrNull()?.id!!)
                    }
                }
        }
    }

    override suspend fun subscribeToList(id: String): Result<Unit> {
        return suspendRunCatching {
            postgrest["subscription"]
                .insert(
                    Subscription(auth.currentUserOrNull()!!.id, id)
                )
        }
    }

    override suspend fun addShowToFavorites(show: TVShow): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[FAVORITE_MOVIES]
                .insert(
                    FavoriteMovieInsert(
                        auth.currentUserOrNull()!!.id,
                        show.posterUrl?.substringAfterLast('/'),
                        show.title,
                        show.overview,
                        show.popularity,
                        -1,
                        show.id,
                    )
                )
        }
            .isSuccess
    }

    override suspend fun addMovieToFavoritesList(movie: Movie): Boolean =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest[FAVORITE_MOVIES]
                    .insert(
                        FavoriteMovieInsert(
                            auth.currentUserOrNull()!!.id,
                            movie.posterUrl?.substringAfterLast('/'),
                            movie.title,
                            movie.overview,
                            movie.popularity,
                            movie.id,
                            -1
                        )
                    )
            }
                .isSuccess
        }

    override suspend fun selectAllItemsForList(listId: String): Result<List<ListItem>> =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest[LIST_ITEM]
                    .select {
                        filter { eq("list_id", listId) }
                    }
                    .decodeList<ListItem>()
            }
        }

    override suspend fun selectListWithItemsById(listId: String): Result<ListWithItems> =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest[USER_LIST]
                    .select(
                        columns = Columns.raw("*, listitem(*)")
                    ) {
                        limit(1)
                        filter { eq("list_id", listId) }
                    }
                    .decodeSingle<ListWithItems>()
            }
        }

    override suspend fun selectListById(listId: String): Result<UserList> =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest[USER_LIST]
                    .select {
                        filter { eq("list_id", listId) }
                    }
                    .decodeSingle<UserList>()
            }
        }

    override suspend fun selectRecommendedFromSubscriptions(): Result<List<ListWithPostersRpcResponse>> =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest.moreFromSubscribed(auth.currentUserOrNull()!!.id, limit = 10, offset = 0)
                    .decodeList<ListWithPostersRpcResponse>()
                    .filterUniqueBy { it.listId }
            }
        }

    override suspend fun selectSubscriptions(userId: String): Result<List<ListWithItems>> =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest.subscribedListWithItems(userId)
                    .decodeList<SubscriptionWithItem>()
                    .toListWithItems()
            }
        }

    override suspend fun selectListsByUserId(userId: String): Result<List<ListWithItems>> =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest[USER_LIST]
                    .select(
                        Columns.raw("*, listitem(*)")
                    ) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<ListWithItems>()
            }
        }

    override suspend fun deleteFromFavorites(id: Long, type: ContentType): Boolean =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest[FAVORITE_MOVIES]
                    .delete {
                        filter {
                            when (type) {
                                ContentType.Movie -> eq("movie_id", id)
                                ContentType.Show -> eq("show_id", id)
                            }
                            eq("user_id", auth.currentUserOrNull()!!.id)
                        }
                    }
            }
                .isSuccess
        }

    override suspend fun updateList(update: UserListUpdate): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[USER_LIST]
                .update(
                    {
                        if (update.name != null) {
                            set("name", update.name)
                        }
                        if (update.public != null) {
                            set("public", update.public)
                        }
                        if (update.description != null) {
                            set("description", update.description)
                        }
                    }
                ) {
                    filter {
                        eq("list_id", update.listId)
                    }
                }
        }
            .isSuccess
    }

    override suspend fun deleteList(listId: String): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[USER_LIST].delete {
                filter { eq("list_id", listId) }
            }
        }
            .isSuccess
    }

    override suspend fun insertList(name: String): Result<UserList> = withContext(ioDispatcher) {
        suspendRunCatching {
            val now = Clock.System.now()
            postgrest[USER_LIST].insert(
                UserList(
                    listId = UUID.randomUUID().toString(),
                    userId = auth.currentUserOrNull()!!.id,
                    name = name,
                    createdAt = now,
                    updatedAt = now,
                    public = false,
                    description = "",
                    subscribers = 0
                )
            ) {
                select()
            }
                .decodeSingle<UserList>()
        }
    }

    override suspend fun deleteFromList(
        id: Long,
        contentList: ContentList,
        type: ContentType
    ): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[LIST_ITEM]
                .delete {
                    filter {
                        when (type) {
                            ContentType.Movie -> {
                                eq("movie_id", id)
                            }

                            ContentType.Show -> {
                                eq("show_id", id)
                            }
                        }
                        eq("list_id", contentList.supabaseId!!)
                        eq("user_id", auth.currentUserOrNull()?.id!!)
                    }
                }
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    override suspend fun addToList(
        id: Long,
        posterPath: String?,
        title: String,
        contentList: ContentList,
        type: ContentType
    ): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[LIST_ITEM]
                .insert(
                    ListItem(
                        listId = contentList.supabaseId!!,
                        userId = auth.currentUserOrNull()!!.id,
                        movieId = type.movieId(id),
                        showId = type.showId(id),
                        posterPath = posterPath,
                        title = title,
                        createdAt = Clock.System.now()
                    )
                )
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    override suspend fun unlikeComment(commentId: Long): Result<CLike> = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[CLIKES]
                .delete {
                    select()
                    filter {
                        eq("cid", commentId)
                        eq("user_id", auth.currentUserOrNull()!!.id)
                    }
                }
                .decodeSingle<CLike>()
        }
    }

    override suspend fun sendReply(commentId: Long, message: String) = withContext(ioDispatcher) {
        suspendRunCatching<Unit> {

            if (message.isEmpty())
                error("text empty")

            postgrest[REPLY]
                .insert(
                    SendReply(message, commentId)
                )
        }
    }

    override suspend fun getMostRecentComment(
        movieId: Long,
        showId: Long
    ): Result<Pair<Long, CommentWithUser>> = withContext(ioDispatcher) {
        suspendRunCatching {
            val result = postgrest[COMMENT]
                .select(
                    columns = Columns.raw(
                        "id, created_at, user_id, message, $USERS:$USERS!${COMMENT}_user_id_fkey(username, profile_image)"
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

            result.countOrNull()!! to result.decodeSingle<CommentWithUser>()
        }
    }

    override suspend fun sendComment(message: String, movieId: Long, showId: Long) =
        withContext(ioDispatcher) {
            suspendRunCatching<Unit> {

                if (message.isEmpty())
                    error("text empty")

                if (movieId == -1L && showId == -1L)
                    error("invalid content id's")

                postgrest[COMMENT]
                    .insert(
                        SendComment(
                            message = message,
                            movieId = movieId,
                            showId = showId,
                        )
                    )
            }
        }

    override suspend fun likeComment(commentId: Long): Result<CLike> =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest[CLIKES]
                    .insert(
                        CLike(
                            userId = auth.currentUserOrNull()!!.id,
                            cid = commentId
                        )
                    ) {
                        select()
                    }
                    .decodeSingle<CLike>()
            }
        }


    override suspend fun getRepliesForComment(commentId: Long): Result<List<ReplyWithUser>> =
        withContext(ioDispatcher) {
            suspendRunCatching {
                postgrest[REPLY]
                    .select(
                        columns = Columns.raw("*, $USERS:$USERS!${REPLY}_user_id_fkey(username, profile_image)")
                    ) {
                        order(column = "created_at", order = Order.DESCENDING)
                        filter {
                            eq("cid", commentId)
                        }
                    }
                    .decodeList<ReplyWithUser>()
            }
        }

    override fun pagingSource(
        pagedType: CommentsPagedType,
        contentId: Long,
        contentType: ContentType
    ): PagingSource<Int, PagedComment> {
        return CommentPagingSource(
            postgrest,
            pagedType,
            contentType.movieId(contentId),
            contentType.showId(contentId),
            auth.currentUserOrNull()?.id
        )
    }

    companion object {
        const val TABLE_USERS = "users"
    }
}

enum class ContentType {
    Movie,
    Show;

    fun movieId(id: Long) = when (this) {
        Movie -> id
        Show -> -1
    }

    fun showId(id: Long) = when (this) {
        Movie -> -1
        Show -> id
    }
}