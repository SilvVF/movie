package io.silv.movie.data.prefrences.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.firstOrNull

/**
 * Modified from https://github.com/aniyomiorg/aniyomi AndroidPreference
 * to use datastore instead of SharedPreferences
 */
class DatastorePreferenceStore(
    private val datastore: DataStore<Preferences>
) : PreferenceStore {

    override fun getString(key: String, defaultValue: String): Preference<String> {
        return DataStorePreference.StringPrimitive(datastore, key, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> {
        return DataStorePreference.LongPrimitive(datastore, key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> {
        return DataStorePreference.IntPrimitive(datastore, key, defaultValue)
    }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> {
        return DataStorePreference.FloatPrimitive(datastore, key, defaultValue)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
        return DataStorePreference.BooleanPrimitive(datastore, key, defaultValue)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
        return DataStorePreference.StringSetPrimitive(datastore, key, defaultValue)
    }

    override fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T
    ): Preference<T> {
        return DataStorePreference.ObjectPrimitive(
            datastore,
            key,
            defaultValue,
            serializer,
            deserializer
        )
    }

    override suspend fun getAll(): Map<Preferences.Key<*>, Any> {
        return datastore.data.firstOrNull()?.asMap() ?: emptyMap()
    }
}