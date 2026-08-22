package com.andrii.patephone.Settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
class SettingsManager(private val androidContext: Context) {
    private val METADATA_ARTWORK_KEY = booleanPreferencesKey("metadata_artwork") // True by default
    val useMetadataArtwork: Flow<Boolean> = androidContext.dataStore.data.map {
        preferences -> preferences[METADATA_ARTWORK_KEY] ?: true
    }

    fun setMetadataArtwork(enabled: Boolean) = CoroutineScope(Dispatchers.IO).launch {
        androidContext.dataStore.edit {
            preferences -> preferences[METADATA_ARTWORK_KEY] = enabled
        }
    }
}