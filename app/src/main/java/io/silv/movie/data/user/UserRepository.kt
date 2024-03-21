package io.silv.movie.data.user

import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SignOutScope
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import timber.log.Timber

interface UserRepository {
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
): UserRepository {

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
                filter { User::userId eq id }
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
                    filter { User::userId eq user.userId }
                    limit(count = 1)
                    order(column = "user_id", Order.DESCENDING)
                }
                .decodeSingle<User>()
        }
            .onFailure { Timber.e(it) }
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