package io.silv.movie.data.user

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.SignOutScope
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.silv.movie.core.NetworkMonitor
import io.silv.movie.data.prefrences.BasePreferences
import io.silv.movie.data.prefrences.core.getOrDefaultBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber

interface UserRepository {
    val currentUser: StateFlow<User?>
    suspend fun deleteAccount(): Boolean
    suspend fun signOut(scope: SignOutScope = SignOutScope.LOCAL): Boolean
    suspend fun getUser(id: String): User?
    suspend fun updateUser(user: User): User?
    suspend fun registerWithEmailAndPassword(email: String, password: String): Boolean
    suspend fun signInWithEmailAndPassword(email: String, password: String): Boolean
    suspend fun resetPassword(email: String): Boolean
}

class UserRepositoryImpl(
    private val postgrest: Postgrest,
    private val auth: Auth,
    networkMonitor: NetworkMonitor,
    basePreferences: BasePreferences
): UserRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val savedUser = basePreferences.savedUser()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser
        .map { user ->
            val uid = auth.currentUserOrNull()?.id

            if (user == null) {
                val saved = savedUser.getOrDefaultBlocking()

                return@map saved.takeIf { uid != null && savedUser.get()?.userId == uid }
            }

            user.takeIf { uid != null }
        }
        .stateIn(
            scope,
            SharingStarted.Lazily,
            savedUser.getOrDefaultBlocking()
        )




    init {
        auth.sessionStatus
            .combine(networkMonitor.isOnline) { a, b -> a to b }
            .onEach { (status, _) ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = getUser(status.session.user?.id ?: return@onEach)
                        _currentUser.update { user }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        savedUser.set(null)
                        _currentUser.update { null }
                    }
                    else -> {
                        _currentUser.update { null }
                    }
                }
        }
            .launchIn(scope)

        currentUser.onEach { user ->
            user?.let { savedUser.set(it) }
        }
            .launchIn(scope)
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

    override suspend fun getUser(id: String): User? {
        return runCatching {
            postgrest[TABLE_USERS].select {
                filter { eq("user_id", id) }
                limit(1)
                order(column = "user_id", Order.DESCENDING)
            }
                .decodeSingle<User>()
        }
            .onFailure { Timber.e(it) }
            .getOrNull()
    }

    override suspend fun updateUser(user: User): User? {
        return runCatching {
            postgrest[TABLE_USERS]
                .update(user) {
                    select()
                    filter { eq("user_id", user.userId) }
                    limit(count = 1)
                    order(column = "user_id", Order.DESCENDING)
                }
                .decodeSingle<User>()
        }
            .onFailure { Timber.e(it) }
            .onSuccess { user ->
                Timber.e(user.toString())
                _currentUser.update { user }
            }
            .getOrNull()
    }

    override suspend fun registerWithEmailAndPassword(email: String, password: String): Boolean {
        return runCatching {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Boolean {
       return runCatching {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }
           .onFailure { Timber.e(it) }
           .isSuccess
    }

    override suspend fun resetPassword(email: String): Boolean {
        return runCatching {
            auth.resetPasswordForEmail(email)
        }
            .onFailure { Timber.e(it) }
            .isSuccess
    }


    companion object {
        const val TABLE_USERS = "users"
    }
}