package com.andrii.patephone.features.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Suppress("PrivatePropertyName")
class SettingsManager(private val androidContext: Context) {
    private val RECURSIVE_IMPORT_KEY = booleanPreferencesKey("recursive_import")
    private val RECURSIVE_IMPORT_DEPTH_KEY = intPreferencesKey("recursive_import_depth")
    private val METADATA_ARTWORK_KEY = booleanPreferencesKey("metadata_artwork")
    private val NOTIFICATION_HEADER_KEY = intPreferencesKey("notification_header")

    enum class NotificationHeader {
        FileName,
        SongTitle
    }

    val useMetadataArtwork: Flow<Boolean> = androidContext.dataStore.data.map { preferences ->
        preferences[METADATA_ARTWORK_KEY] ?: true
    }
    val recursiveImport: Flow<Boolean> = androidContext.dataStore.data.map { preferences ->
        preferences[RECURSIVE_IMPORT_KEY] ?: false
    }
    val recursiveImportDepth: Flow<Int> = androidContext.dataStore.data.map { preferences ->
        preferences[RECURSIVE_IMPORT_DEPTH_KEY] ?: 1
    }
    val notificationHeader: Flow<NotificationHeader> = androidContext.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_HEADER_KEY]?.let { NotificationHeader.entries[it] } ?: NotificationHeader.FileName
    }

    fun setMetadataArtwork(enabled: Boolean) = CoroutineScope(Dispatchers.IO).launch {
        androidContext.dataStore.edit { preferences ->
            preferences[METADATA_ARTWORK_KEY] = enabled
        }
    }

    fun setRecursiveImport(enabled: Boolean) = CoroutineScope(Dispatchers.IO).launch {
        androidContext.dataStore.edit { preferences ->
            preferences[RECURSIVE_IMPORT_KEY] = enabled
        }
    }

    fun setRecursiveImportDepth(value: Int) = CoroutineScope(Dispatchers.IO).launch {
        androidContext.dataStore.edit { preferences ->
            preferences[RECURSIVE_IMPORT_DEPTH_KEY] = value
        }
    }

    fun setNotificationHeader(header: NotificationHeader) = CoroutineScope(Dispatchers.IO).launch {
        androidContext.dataStore.edit { preferences ->
            preferences[NOTIFICATION_HEADER_KEY] = header.ordinal
        }
    }
}
