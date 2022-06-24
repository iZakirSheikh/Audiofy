package com.prime.player.preferences

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException
import androidx.datastore.preferences.core.Preferences as dPref


private const val TAG = "Preferences"

private const val PREFERENCE_NAME = "Shared_preferences"

private val Context.store: DataStore<dPref> by preferencesDataStore(
    name = PREFERENCE_NAME
)

private class PreferencesImpl(context: Application) : Preferences {
    private val store = context.store
    override suspend fun setString(key: String, value: String) {
        store.edit { pref ->
            pref[stringPreferencesKey(key)] = value
        }
    }

    override suspend fun setStringSet(key: String, values: Set<String>) {
        store.edit { pref ->
            pref[stringSetPreferencesKey(key)] = values
        }
    }

    override suspend fun setInt(key: String, value: Int) {
        store.edit { pref ->
            pref[intPreferencesKey(key)] = value
        }
    }

    override suspend fun setLong(key: String, value: Long) {
        store.edit { pref ->
            pref[longPreferencesKey(key)] = value
        }
    }

    override suspend fun setFloat(key: String, value: Float) {
        store.edit { pref ->
            pref[floatPreferencesKey(key)] = value
        }
    }

    override suspend fun setBoolean(key: String, value: Boolean) {
        store.edit { pref ->
            pref[booleanPreferencesKey(key)] = value
        }
    }

    override suspend fun remove(key: dPref.Key<*>) {
        store.edit { pref ->
            pref.minusAssign(key)
        }
    }

    override fun getString(key: String, defValue: String) = store.data.catch { exception ->
        if (exception is IOException) {
            Log.e(TAG, "getString: $exception")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preference ->
        preference[stringPreferencesKey(key)] ?: defValue
    }

    override fun getStringSet(key: String, defValue: Set<String>) = store.data.catch { exception ->
        if (exception is IOException) {
            Log.e(TAG, "getStringSet: $exception")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preference ->
        preference[stringSetPreferencesKey(key)] ?: defValue
    }

    override fun getInt(key: String, defValue: Int) = store.data.catch { exception ->
        if (exception is IOException) {
            Log.e(TAG, "getInt: $exception")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preference ->
        preference[intPreferencesKey(key)] ?: defValue
    }

    override fun getLong(key: String, defValue: Long) = store.data.catch { exception ->
        if (exception is IOException) {
            Log.e(TAG, "getLong: $exception")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preference ->
        preference[longPreferencesKey(key)] ?: defValue
    }

    override fun getFloat(key: String, defValue: Float) = store.data.catch { exception ->
        if (exception is IOException) {
            Log.e(TAG, "getFloat: $exception")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preference ->
        preference[floatPreferencesKey(key)] ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean) = store.data.catch { exception ->
        if (exception is IOException) {
            Log.e(TAG, "getBoolean: $exception")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preference ->
        preference[booleanPreferencesKey(key)] ?: defValue
    }

    override fun contains(key: dPref.Key<*>) = store.data.catch { exception ->
        if (exception is IOException) {
            Log.e(TAG, "contains: $exception")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preference ->
        preference.contains(key = key)
    }
}


interface Preferences {
    suspend fun setString(key: String, value: String)

    suspend fun setStringSet(key: String, values: Set<String>)

    suspend fun setInt(key: String, value: Int)

    suspend fun setLong(key: String, value: Long)

    suspend fun setFloat(key: String, value: Float)

    suspend fun setBoolean(key: String, value: Boolean)

    suspend fun remove(key: dPref.Key<*>)

    fun getString(key: String, defValue: String): Flow<String>

    fun getStringSet(key: String, defValue: Set<String>): Flow<Set<String>>

    fun getInt(key: String, defValue: Int): Flow<Int>

    fun getLong(key: String, defValue: Long): Flow<Long>

    fun getFloat(key: String, defValue: Float): Flow<Float>

    fun getBoolean(key: String, defValue: Boolean): Flow<Boolean>

    fun contains(key: dPref.Key<*>): Flow<Boolean>

    fun <T> Flow<T>.collectBlocking(): T = runBlocking { first() }

    @Composable
    fun <T> Flow<T>.collectAsState(): State<T> =
        collectAsState(initial = collectBlocking())

    companion object {
        // Singleton prevents multiple instances of repository opening at the
        // same time.
        private const val TAG = "Preferences"

        @Volatile
        private var INSTANCE: PreferencesImpl? = null

        fun get(context: Context): Preferences {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the repository
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesImpl(context.applicationContext as Application)
                INSTANCE = instance
                instance
            }
        }
    }
}