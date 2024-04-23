/*
Copyright 2015 Javier Tomás

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package io.silv.movie.data.prefrences.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Modified from https://github.com/aniyomiorg/aniyomi AndroidPreference
 * to use datastore instead of SharedPreferences
 */
sealed class DataStorePreference<T>(
    protected val dataStore: DataStore<Preferences>,
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {

    abstract suspend fun read(datastore: DataStore<Preferences>, key: String, defaultValue: T): T

    abstract suspend fun write(key: String, value: T)

    abstract fun changesFlow(datastore: DataStore<Preferences>, key: String, defaultValue: T): Flow<T>

    abstract suspend fun delete(key: String)

    override fun key(): String { return key }

    override suspend fun get(): T {
        return try {
            read(dataStore, key, defaultValue)
        } catch (e: ClassCastException) {
            delete()
            defaultValue
        }
    }

    override suspend fun set(value: T) { write(key, value) }

    override suspend fun isSet(): Boolean {
        return dataStore.data
            .firstOrNull()
            ?.asMap()
            ?.any { entry -> entry.key.name == key }
            ?: false
    }

    override fun defaultValue(): T {
        return defaultValue
    }

    override suspend fun delete() { delete(key) }

    override fun changes(): Flow<T> {
        return changesFlow(dataStore, key, defaultValue).conflate()
    }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> {
        return changes().stateIn(
            scope,
            SharingStarted.Eagerly,
            getOrDefaultBlocking()
        )
    }

    class StringSetPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: Set<String>
    ) : DataStorePreference<Set<String>>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Set<String>
        ): Flow<Set<String>> {
            return dataStore.data.map { it[stringSetPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Set<String>,
        ):  Set<String> {
            return dataStore.data.firstOrNull()?.get(stringSetPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(stringSetPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  Set<String>) {
            dataStore.edit { it[stringSetPreferencesKey(key)] = value }
        }
    }

    class StringPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: String,
    ) : DataStorePreference<String>(preferences, key, defaultValue) {

        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: String
        ): Flow<String> {
            return dataStore.data.map { it[stringPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: String,
        ):  String {
            return dataStore.data.firstOrNull()?.get(stringPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(stringPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  String) {
            dataStore.edit { it[stringPreferencesKey(key)] = value }
        }
    }

    class LongPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: Long,
    ) : DataStorePreference<Long>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Long
        ): Flow<Long> {
            return dataStore.data.map { it[longPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Long,
        ):  Long {
            return dataStore.data.firstOrNull()?.get(longPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(longPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  Long) {
            dataStore.edit { it[longPreferencesKey(key)] = value }
        }
    }

    class IntPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: Int,
    ) : DataStorePreference<Int>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Int
        ): Flow<Int> {
            return dataStore.data.map { it[intPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Int,
        ):  Int {
            return dataStore.data.firstOrNull()?.get(intPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(intPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  Int) {
            dataStore.edit { it[intPreferencesKey(key)] = value }
        }
    }

    class FloatPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: Float,
    ) : DataStorePreference<Float>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Float
        ): Flow<Float> {
            return dataStore.data.map { it[floatPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Float,
        ):  Float {
            return dataStore.data.firstOrNull()?.get(floatPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(floatPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  Float) {
            dataStore.edit { it[floatPreferencesKey(key)] = value }
        }
    }

    class BooleanPrimitive(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: Boolean,
    ) : DataStorePreference<Boolean>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Boolean
        ): Flow<Boolean> {
            return dataStore.data.map { it[booleanPreferencesKey(key)] ?: defaultValue }
        }

        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: Boolean,
        ):  Boolean {
            return dataStore.data.firstOrNull()?.get(booleanPreferencesKey(key)) ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(booleanPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  Boolean) {
            dataStore.edit { it[booleanPreferencesKey(key)] = value }
        }
    }

    class ObjectPrimitive<T>(
        preferences: DataStore<Preferences>,
        key: String,
        defaultValue: T,
        val serializer: (T) -> String,
        val deserializer: (String) -> T,
    ) : DataStorePreference<T>(preferences, key, defaultValue) {
        override fun changesFlow(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: T
        ): Flow<T> {
            return dataStore.data.map { prefs ->
                prefs[stringPreferencesKey(key)]?.let { deserializer(it) } ?: defaultValue
            }
        }
        override suspend fun read(
            datastore: DataStore<Preferences>,
            key: String,
            defaultValue: T,
        ):  T {
            val string = dataStore.data.firstOrNull()?.get(stringPreferencesKey(key))
            return string?.let { deserializer(it) } ?: defaultValue
        }

        override suspend fun delete(key: String) {
            dataStore.edit { it.remove(stringPreferencesKey(key)) }
        }
        override suspend fun write(key: String, value:  T) {
            dataStore.edit { it[stringPreferencesKey(key)] = serializer(value) }
        }
    }
}