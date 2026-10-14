package com.andrii.patephone.features.main

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.andrii.patephone.core.TAG_VIEW_MODEL
import com.andrii.patephone.core.data.PlayerState
import com.andrii.patephone.core.data.PlaylistItem
import com.andrii.patephone.core.data.Song
import com.andrii.patephone.core.player.MediaItemBuilder
import com.andrii.patephone.core.player.MusicServiceConnection
import com.andrii.patephone.core.player.PlayerAction
import com.andrii.patephone.features.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val className: String = this::class.java.simpleName
    private val _playlist = MutableStateFlow(emptyArray<PlaylistItem>())

    val playlist = _playlist.asStateFlow()
    val playerState = MusicServiceConnection.playerState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerState(
            currentSongIndex = 0
        )
    )
    val song = MusicServiceConnection.song.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Song()
    )

    fun togglePlay() {
        if (playerState.value.isPlaying) {
            MusicServiceConnection.pause()
            MusicServiceConnection.stopTracking()
        } else {
            MusicServiceConnection.play()
            MusicServiceConnection.startTracking()
        }
        Log.d(TAG_VIEW_MODEL, "Toggle play pressed")
    }

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    fun onAction(action: PlayerAction) {
        when (action) {
            is PlayerAction.PlayPause -> togglePlay()
            is PlayerAction.SkipNext -> MusicServiceConnection.skipToNext()
            is PlayerAction.SkipPrevious -> MusicServiceConnection.skipToPrevious()
            is PlayerAction.ToggleShuffle -> MusicServiceConnection.toggleShuffle()
            is PlayerAction.ToggleRepeat -> MusicServiceConnection.toggleRepeat()
            else -> throw IllegalArgumentException("Unexpected action type")
        }
    }

    fun getShowTimeAboveSlider(): Boolean {
        val context = getApplication<Application>().applicationContext
        return runBlocking {
            SettingsManager(context).showTimeAboveSlider.first()
        }
    }

    fun onActionImport(treeUri: Uri?) {
        val context = getApplication<Application>().applicationContext
        if (treeUri == null) return
        MusicServiceConnection.stop()
        MusicServiceConnection.clearMediaItems()
        MusicServiceConnection.fallbackPictures.clear()
        MusicServiceConnection.customArtwork = null

        viewModelScope.launch(Dispatchers.IO) {
            val recursiveImport: Boolean = SettingsManager(context).recursiveImport.first()
            // this method returns pair, where first value is MediaItem arraylist, and second value is it's names typed array
            val fileDataPairs = listAudioFiles(
                treeUri = treeUri,
                context = context,
                recursiveImportDepth = if (recursiveImport) getRecursiveImportDepth(context) else 0,
                currentDepth = 0
            )
            val playlist = fileDataPairs.first
            viewModelScope.launch(Dispatchers.Main) {
                MusicServiceConnection.addMediaItems(playlist)
                _playlist.value = fileDataPairs.second
                val fallbacks = MusicServiceConnection.fallbackPictures
                MusicServiceConnection.customArtwork = fallbacks.firstOrNull()?.uri
                MusicServiceConnection.lazyEnrichment(context)
            }
        }
    }

    private suspend fun getRecursiveImportDepth(context: Context): Int {
        val property =
            SettingsManager(context).recursiveImportDepth.first()
        return property.also { Log.d(className, "Got recursive import property: $it") }
    }

    // this method returns , where first value is MediaItem arraylist, the second value
    // is it's names typed array, and the third is next index (used for recursive import)
    suspend fun listAudioFiles(
        treeUri: Uri,
        context: Context,
        recursiveImportDepth: Int,
        currentDepth: Int,
        lastIndex: Int = 0
    ): Triple<ArrayList<MediaItem>, Array<PlaylistItem>, Int> {
        val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
        val playlist = ArrayList<MediaItem>()
        val names = ArrayList<PlaylistItem>()
        var currentIndex = lastIndex

        try {
            val files = pickedDir?.listFiles()?.apply { sortBy { it.name ?: "" } } ?: return Triple(
                ArrayList<MediaItem>(),
                emptyArray(),
                0
            )

            for (file in findFolderArtworks(files)) {
                MusicServiceConnection.fallbackPictures.add(file)
            }

            for (file in files) {
                if (file.isFile && (file.type?.startsWith("audio/") == true) && (file.name?.substringAfterLast(
                        ".",
                        ""
                    ) !in MusicServiceConnection.UNSUPPORTED_TYPES)
                ) {
                    val mediaID = file.uri.hashCode().toString()
                    val mediaItem = MediaItemBuilder(
                        context,
                        MusicServiceConnection.customArtwork,
                        mediaID
                    ).freshBuild(file)
                    playlist.add(mediaItem)
                    names.add(
                        PlaylistItem.SongItem(
                            value = (mediaItem.mediaMetadata.title
                                ?: mediaItem.mediaMetadata.displayTitle
                                ?: "Unknown") as String,
                            index = currentIndex++
                        )
                    )
                } else if (file.isDirectory && currentDepth < recursiveImportDepth) {
                    val files = withContext(Dispatchers.IO) {
                        listAudioFiles(
                            file.uri,
                            context,
                            recursiveImportDepth + 1,
                            currentDepth + 1,
                            currentIndex
                        )
                    }
                    val songsFound = files.first
                    currentIndex = files.third
                    if (songsFound.isNotEmpty()) {
                        names.add(PlaylistItem.FolderTitle(file.name ?: "Inner directory"))
                    }
                    songsFound.forEach { playlist.add(it) }
                    files.second.forEach { names.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(className, "Something went wrong by import: ${e.printStackTrace()}")
        }
        return Triple(playlist, names.toTypedArray(), currentIndex)
    }

    fun findFolderArtworks(files: Array<DocumentFile>): ArrayList<DocumentFile> {
        val foundedPictures = ArrayList<DocumentFile>()
        files.forEach {
            val name = it.name?.lowercase() ?: ""
            if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
                foundedPictures.add(it)
            }
        }
        Log.d(className, "Uri found in dir: ${foundedPictures.size}")
        return foundedPictures
    }

    fun onSliderMove(float: Float) {
        val duration = MusicServiceConnection.getDuration()
        MusicServiceConnection.seekTo((duration * float).toLong())
    }

    override fun onCleared() {
        _playlist.value = emptyArray<PlaylistItem>()
        MusicServiceConnection.stopTracking()
        super.onCleared()
    }

    fun seekToMedia(index: Int) {
        MusicServiceConnection.seekToMediaItem(index)
    }

}
