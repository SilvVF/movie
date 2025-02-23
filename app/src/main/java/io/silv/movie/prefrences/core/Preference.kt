package io.silv.movie.prefrences.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull


interface Preference<T> {

    fun key(): String

    suspend fun get(): T

    suspend fun set(value: T)

    suspend fun isSet(): Boolean

    suspend fun delete()

    fun defaultValue(): T

    fun changes(): Flow<T>

    fun stateIn(scope: CoroutineScope): StateFlow<T>

    companion object {
        /**
         * A preference that should not be exposed in places like backups without user consent.
         */
        fun isPrivate(key: String): Boolean {
            return key.startsWith(PRIVATE_PREFIX)
        }
        fun privateKey(key: String): String {
            return "$PRIVATE_PREFIX$key"
        }
        /**
         * A preference used for internal app state that isn't really a user preference
         * and therefore should not be in places like backups.
         */
        fun isAppState(key: String): Boolean {
            return key.startsWith(APP_STATE_PREFIX)
        }
        fun appStateKey(key: String): String {
            return "$APP_STATE_PREFIX$key"
        }

        private const val APP_STATE_PREFIX = "__APP_STATE_"
        private const val PRIVATE_PREFIX = "__PRIVATE_"
    }
}

inline fun <reified T : Enum<T>> PreferenceStore.getEnum(
    key: String,
    defaultValue: T,
): Preference<T> {
    return getObject(
        key = key,
        defaultValue = defaultValue,
        serializer = { it.name },
        deserializer = {
            try {
                enumValueOf(it)
            } catch (e: IllegalArgumentException) {
                defaultValue
            }
        }
    )
}

suspend inline fun <reified T, R : T> Preference<T>.getAndSet(crossinline block: (T) -> R) = set(
    block(get()),
)

suspend operator fun <T> Preference<Set<T>>.plusAssign(item: T) {
    set(get() + item)
}

suspend operator fun <T> Preference<Set<T>>.minusAssign(item: T) {
    set(get() - item)
}

suspend fun Preference<Boolean>.toggle(): Boolean {
    set(!get())
    return get()
}