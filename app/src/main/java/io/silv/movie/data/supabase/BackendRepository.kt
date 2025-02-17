package io.silv.movie.data.supabase

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.silv.movie.IoDispatcher
import io.silv.movie.core.NetworkMonitor
import io.silv.movie.core.filterUniqueBy
import io.silv.movie.core.suspendRunCatching
import io.silv.movie.data.model.ContentList
import io.silv.movie.data.model.ListWithPostersRpcResponse
import io.silv.movie.data.model.Movie
import io.silv.movie.data.model.TVShow
import io.silv.movie.data.supabase.SupabaseConstants.FAVORITE_MOVIES
import io.silv.movie.data.supabase.SupabaseConstants.LIST_ITEM
import io.silv.movie.data.supabase.SupabaseConstants.RPC.moreFromSubscribed
import io.silv.movie.data.supabase.SupabaseConstants.RPC.subscribedListWithItems
import io.silv.movie.data.supabase.SupabaseConstants.SUBSCRIPTION
import io.silv.movie.data.supabase.SupabaseConstants.USER_LIST
import io.silv.movie.data.supabase.model.FavoriteMovie
import io.silv.movie.data.supabase.model.FavoriteMovieInsert
import io.silv.movie.prefrences.BasePreferences
import io.silv.movie.data.supabase.model.User
import io.silv.movie.data.supabase.model.list.ListItem
import io.silv.movie.data.supabase.model.list.ListWithItems
import io.silv.movie.data.supabase.model.list.UserList
import io.silv.movie.data.supabase.model.list.UserListUpdate
import io.silv.movie.data.supabase.model.subscription.Subscription
import io.silv.movie.data.supabase.model.subscription.SubscriptionWithItem
import io.silv.movie.data.supabase.model.toListWithItems
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
    suspend fun listenForUpdates(): Job
    suspend fun deleteAccount(): Boolean
    suspend fun signOut(scope: SignOutScope = SignOutScope.LOCAL): Boolean
    suspend fun getUser(id: String): Result<User>
    suspend fun updateUser(user: User): Result<User>
    suspend fun registerWithEmailAndPassword(email: String, password: String): Boolean
    suspend fun signInWithEmailAndPassword(email: String, password: String): Boolean
    suspend fun resetPassword(email: String): Boolean
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
    suspend fun addToList(id: Long, posterPath: String?, title: String, contentList: ContentList, type: ContentType): Boolean
}

interface BackendRepository: UserRepository, ListRepository

sealed interface BackendEvent {
    data object UserUpdated: BackendEvent
}

class BackendRepositoryImpl(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val networkMonitor: NetworkMonitor,
    private val basePreferences: BasePreferences,
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

    override suspend fun listenForUpdates() = supervisorScope {
        auth.sessionStatus
                .combine(networkMonitor.isOnline, ::Pair)
                .onEach { (status, online) ->

                    if (!online) return@onEach

                    when (status) {
                        is SessionStatus.Authenticated -> {
                            val id = status.session.user?.id ?: return@onEach
                            getUser(id).onSuccess  {
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

        events.receiveAsFlow().onEach { event ->
            when(event) {
                BackendEvent.UserUpdated -> launch {
                    auth.currentUserOrNull()?.let {
                        getUser(it.id)
                    }
                }
            }
        }
            .catch { Timber.e(it) }
            .launchIn(this)
    }


    override suspend fun deleteAccount(): Boolean {
        return runCatching {
            postgrest.rpc("deleteUser")
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    override suspend fun signOut(scope: SignOutScope): Boolean {
        return runCatching {
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

    override suspend fun updateUser(user: User): Result<User>  = withContext(ioDispatcher) {
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
    }

    override suspend fun registerWithEmailAndPassword(email: String, password: String): Boolean = withContext(ioDispatcher) {
        suspendRunCatching{
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    override suspend fun resetPassword(email: String): Boolean  = withContext(ioDispatcher) {
        suspendRunCatching {
            auth.resetPasswordForEmail(email)
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    override suspend fun selectFavoritesList(userId: String): Result<List<FavoriteMovie>> = withContext(ioDispatcher) {
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
        return runCatching {
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

    override suspend fun addMovieToFavoritesList(movie: Movie): Boolean = withContext(ioDispatcher) {
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

    override suspend fun selectAllItemsForList(listId: String): Result<List<ListItem>> = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[LIST_ITEM]
                .select {
                    filter { eq("list_id", listId) }
                }
                .decodeList<ListItem>()
        }
    }

    override suspend fun selectListWithItemsById(listId: String): Result<ListWithItems> = withContext(ioDispatcher) {
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

    override suspend fun selectListById(listId: String): Result<UserList> = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[USER_LIST]
                .select {
                    filter { eq("list_id", listId) }
                }
                .decodeSingle<UserList>()
        }
    }

    override suspend fun selectRecommendedFromSubscriptions(): Result<List<ListWithPostersRpcResponse>> = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest.moreFromSubscribed(auth.currentUserOrNull()!!.id, limit = 10, offset = 0)
                .decodeList<ListWithPostersRpcResponse>()
                .filterUniqueBy { it.listId }
        }
    }

    override suspend fun selectSubscriptions(userId: String): Result<List<ListWithItems>> = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest.subscribedListWithItems(userId)
                .decodeList<SubscriptionWithItem>()
                .toListWithItems()
        }
    }

    override suspend fun selectListsByUserId(userId: String): Result<List<ListWithItems>> = withContext(ioDispatcher) {
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

    override suspend fun deleteFromFavorites(id: Long, type: ContentType): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[FAVORITE_MOVIES]
                .delete {
                    filter {
                        when(type) {
                            ContentType.Movie -> eq("movie_id", id)
                            ContentType.Show -> eq("show_id", id)
                        }
                        eq("user_id", auth.currentUserOrNull()!!.id)
                    }
                }
        }
            .isSuccess
    }

    override suspend fun updateList(update: UserListUpdate): Boolean = withContext(ioDispatcher){
        suspendRunCatching {
            postgrest[USER_LIST]
                .update(
                    {
                        if (update.name != null) { set("name", update.name) }
                        if (update.public != null) { set("public", update.public) }
                        if (update.description != null) { set("description", update.description) }
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

    override suspend fun deleteFromList(id: Long , contentList: ContentList, type: ContentType): Boolean = withContext(ioDispatcher) {
        suspendRunCatching {
            postgrest[LIST_ITEM]
                .delete {
                    filter {
                        when(type) {
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

    override suspend fun addToList(id: Long, posterPath: String?, title: String, contentList: ContentList, type: ContentType): Boolean = withContext(ioDispatcher) {
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
    companion object {
        const val TABLE_USERS = "users"
    }
}

enum class ContentType {
    Movie,
    Show;

    fun movieId(id: Long) = when(this) {
        Movie -> id
        Show -> -1
    }
    fun showId(id: Long) = when(this) {
        Movie -> -1
        Show -> id
    }
}