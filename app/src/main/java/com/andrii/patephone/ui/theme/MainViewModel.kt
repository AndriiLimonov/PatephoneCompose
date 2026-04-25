package com.andrii.patephone.ui.theme

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.andrii.patephone.MediaItemBuilder
import com.andrii.patephone.TAG_VIEW_MODEL
import com.andrii.patephone.action.MusicServiceConnection
import com.andrii.patephone.action.PlayerAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ArraySerializer
import javax.inject.Inject

val supportedFiles: Array<String> = arrayOf("mp3", "flac")

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    val progress = musicServiceConnection.progress

    val isShuffleEnabled = musicServiceConnection.isShuffleEnabled
    val repeatMode = musicServiceConnection.repeatMode
    val isPlaying = musicServiceConnection.isPlaying

    private val _imageUri = MutableStateFlow<Uri?>(null)

    val imageUri = _imageUri.asStateFlow()
    val artist = musicServiceConnection.artist
    val title = musicServiceConnection.title

    fun togglePlay() {
        if (isPlaying.value) musicServiceConnection.pause() else musicServiceConnection.play()
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

    fun onActionImport(treeUri: Uri?){
        if (treeUri == null) return
        musicServiceConnection.clearMediaItems()

        viewModelScope.launch (Dispatchers.IO){
            listAudioFiles(treeUri)
        }
    }

    fun listAudioFiles(treeUri: Uri) {
        val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
        val retriever = MediaMetadataRetriever()
        val arr = ArrayList<MediaItem>()

        pickedDir?.listFiles()?.forEach { file ->
            if (file.isFile && (file.type?.startsWith("audio/") == true)) {
                arr.add(MediaItemBuilder(context, retriever).build(file.uri))
            } else if (file.isFile && (file.type?.startsWith("image/") == true)){
                _imageUri.value = file.uri
                Log.d("Common", "image found")
            }
        }

        viewModelScope.launch (Dispatchers.Main){
            for (file in arr){
                musicServiceConnection.addMediaItem(file)
            }
        }
        retriever.release()
    }

    fun onSliderMove(float: Float){
        val duration = musicServiceConnection.getDuration()
        musicServiceConnection.seekTo((duration*float).toLong())
    }

    override fun onCleared() {
        musicServiceConnection.stopTracking()
        super.onCleared()
    }
}
