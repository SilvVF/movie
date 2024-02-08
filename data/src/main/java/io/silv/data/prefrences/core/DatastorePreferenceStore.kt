package io.silv.data.prefrences.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.silv.data.prefrences.core.DataStorePreference.BooleanPrimitive
import io.silv.data.prefrences.core.DataStorePreference.FloatPrimitive
import io.silv.data.prefrences.core.DataStorePreference.IntPrimitive
import io.silv.data.prefrences.core.DataStorePreference.LongPrimitive
import io.silv.data.prefrences.core.DataStorePreference.ObjectPrimitive
import io.silv.data.prefrences.core.DataStorePreference.StringPrimitive
import io.silv.data.prefrences.core.DataStorePreference.StringSetPrimitive
import kotlinx.coroutines.flow.firstOrNull

/**
 * Modified from https://github.com/aniyomiorg/aniyomi AndroidPreference
 * to use datastore instead of SharedPreferences
 */
class DatastorePreferenceStore(
    private val datastore: DataStore<Preferences>
) : PreferenceStore {

    override fun getString(key: String, defaultValue: String): Preference<String> {
        return StringPrimitive(datastore, key, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> {
        return LongPrimitive(datastore, key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> {
        return IntPrimitive(datastore, key, defaultValue)
    }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> {
        return FloatPrimitive(datastore, key, defaultValue)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
        return BooleanPrimitive(datastore, key, defaultValue)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
        return StringSetPrimitive(datastore, key, defaultValue)
    }

    override fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T
    ): Preference<T> {
        return ObjectPrimitive(datastore, key, defaultValue, serializer, deserializer)
    }

    override suspend fun getAll(): Map<Preferences.Key<*>, Any> {
        return datastore.data.firstOrNull()?.asMap() ?: emptyMap()
    }
}