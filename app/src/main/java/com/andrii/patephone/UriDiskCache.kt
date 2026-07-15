package com.andrii.patephone
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.core.net.toUri
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "uri_cache")

class UriDiskCache(private val context: Context) {

    fun getUri(key: String): Flow<Uri?> {
        val prefKey = stringPreferencesKey(key)
        return context.dataStore.data.map { preferences ->
            preferences[prefKey]?.toUri()
        }
    }

    suspend fun putUri(key: String, uri: Uri) {
        val prefKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences[prefKey] = uri.toString()
        }
    }

    suspend fun removeUri(key: String) {
        val prefKey = stringPreferencesKey(key)
        context.dataStore.edit { preferences ->
            preferences.remove(prefKey)
        }
    }

    private fun Preferences.toMap(): Map<String, Uri> {
        val map = mutableMapOf<String, Uri>()
        this.asMap().forEach { (key, value) ->
            if (value is String) {
                map[key.name] = value.toUri()
            }
        }
        return map
    }

    fun getAllUrisFlow(): Flow<Map<String, Uri>> {
        return context.dataStore.data.map { preferences ->
            preferences.toMap()
        }
    }

    suspend fun getAllUrisOnce(): Map<String, Uri> {
        return getAllUrisFlow().first() // берет только первый снимок данных
    }
}