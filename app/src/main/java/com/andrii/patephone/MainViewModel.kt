package com.andrii.patephone

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.preference.PreferenceManager
import com.andrii.patephone.action.MusicServiceConnection
import com.andrii.patephone.action.PlayerAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val _imageUri = MutableStateFlow<Uri?>(null)

    private val _playlist = MutableStateFlow(emptyArray<String>())

    val playlist = _playlist.asStateFlow()

    val artworkUri = musicServiceConnection.artworkUri
    val progress = musicServiceConnection.progress
    val isShuffleEnabled = musicServiceConnection.isShuffleEnabled
    val repeatMode = musicServiceConnection.repeatMode

    val isPlaying = musicServiceConnection.isPlaying
    val artist = musicServiceConnection.artist
    val title = musicServiceConnection.title
    val currentSongIndex = musicServiceConnection.currentSongIndex


    fun togglePlay() {
        if (isPlaying.value) {
            musicServiceConnection.pause()
            musicServiceConnection.stopTracking()
        } else {
            musicServiceConnection.play()
            musicServiceConnection.startTracking()
        }
        Log.d(TAG_VIEW_MODEL, "Toggle play pressed")
    }

    fun onAction(action: PlayerAction) {
        when (action) {
            is PlayerAction.PlayPause -> togglePlay()
            is PlayerAction.SkipNext -> musicServiceConnection.skipToNext()
            is PlayerAction.SkipPrevious -> musicServiceConnection.skipToPrevious()
            is PlayerAction.ToggleShuffle -> musicServiceConnection.toggleShuffle()
            is PlayerAction.ToggleRepeat -> musicServiceConnection.toggleRepeat()
        }
    }

    fun onActionImport(treeUri: Uri?) {
        if (treeUri == null) return
        musicServiceConnection.stop()
        musicServiceConnection.clearMediaItems()

        viewModelScope.launch(Dispatchers.IO) {
            listAudioFiles(treeUri)
        }
    }

    fun listAudioFiles(treeUri: Uri) {
        val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
        val retriever = MediaMetadataRetriever()
        val list = ArrayList<MediaItem>()
        val playlist = ArrayList<String>()

        try {
            val files = pickedDir?.listFiles() ?: return
            val customArtwork = findFolderArtwork(files)
            for (file in files){
                if (file.isFile && (file.type?.startsWith("audio/") == true)) {
                    val mediaItem = MediaItemBuilder(
                        context,
                        retriever,
                        customArtwork,
                        PreferenceManager.getDefaultSharedPreferences(context)
                            .getBoolean("artworkEnable", true),
                        file.uri.hashCode().toString()
                    ).build(file)
                    list.add(mediaItem)
                    playlist.add((mediaItem.mediaMetadata.title ?: "Unknown") as String)
                }
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Something went wrong by import: ${e.printStackTrace()}")
        } finally {
            retriever.release()
        }

        viewModelScope.launch(Dispatchers.Main) {
            musicServiceConnection.addMediaItems(list)
        }
        _playlist.value = playlist.toTypedArray()
    }

    fun findFolderArtwork(files: Array<DocumentFile>): Uri? {
        val standardCover = files.firstOrNull { file ->
            val name = file.name?.lowercase() ?: ""
            name == "cover.jpg" || name == "cover.png" ||
                    name == "folder.jpg" || name == "album.jpg" || name == "artwork.jpg"
        }

        if (standardCover != null) return standardCover.uri

        val anyImage = files.firstOrNull { file ->
            val name = file.name?.lowercase() ?: ""
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
        }

        return anyImage?.uri
    }

    fun onSliderMove(float: Float) {
        val duration = musicServiceConnection.getDuration()
        musicServiceConnection.seekTo((duration * float).toLong())
    }

    override fun onCleared() {
        _playlist.value = emptyArray<String>()
        musicServiceConnection.stopTracking()
        super.onCleared()
    }

    fun seekToMedia(index: Int) {
        musicServiceConnection.seekToMediaItem(index)
    }
}