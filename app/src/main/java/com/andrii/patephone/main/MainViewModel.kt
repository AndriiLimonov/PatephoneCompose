package com.andrii.patephone.main

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.andrii.patephone.MediaItemBuilder
import com.andrii.patephone.PlayerState
import com.andrii.patephone.Song
import com.andrii.patephone.action.MusicServiceConnection
import com.andrii.patephone.action.PlayerAction
import com.andrii.patephone.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainViewModel(
) : ViewModel() {
    val className = "MainViewModel"
    private val _playlist = MutableStateFlow(emptyArray<String>())

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

    fun onAction(action: PlayerAction) {
        when (action) {
            is PlayerAction.PlayPause -> togglePlay()
            is PlayerAction.SkipNext -> MusicServiceConnection.skipToNext()
            is PlayerAction.SkipPrevious -> MusicServiceConnection.skipToPrevious()
            is PlayerAction.ToggleShuffle -> MusicServiceConnection.toggleShuffle()
            is PlayerAction.ToggleRepeat -> MusicServiceConnection.toggleRepeat()
            is PlayerAction.AddToFavs -> addToFavs(MusicServiceConnection.getCurrentMediaItem())
            else -> throw IllegalArgumentException("Unexpected action type")
        }
    }

    fun addToFavs(mediaItem: MediaItem?) {
        TODO("Not yet implemented")
        /* if (mediaItem == null) return
        try {
            val uri: Uri = mediaItem.localConfiguration!!.uri
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val resolver = context.contentResolver
            resolver.takePersistableUriPermission(uri, takeFlags)
            // Put file name as key and song uri
            CoroutineScope(Dispatchers.IO).launch {
                UriDiskCache(context).putUri(
                    key = mediaItem.mediaMetadata.displayTitle as String? ?: DocumentFile.fromSingleUri(context, uri)?.name ?: "Unknown",
                    uri = uri
                )
            }
            Log.d(CLASSNAME, "Successfully cached song as favourite")
        } catch (e: Exception) {
            Log.e(CLASSNAME, "Song wasn't cached: ${e.cause}")
        }
         */
    }

    fun onActionImport(treeUri: Uri?, context: Context) {
        if (treeUri == null) return
        MusicServiceConnection.stop()
        MusicServiceConnection.clearMediaItems()

        viewModelScope.launch(Dispatchers.IO) {
            listAudioFiles(treeUri, context)
//            MusicServiceConnection.lazyEnrichment()
        }
    }

    fun listAudioFiles(treeUri: Uri, context: Context) {
        val recursiveImport: Boolean = runBlocking { SettingsManager(context).recursiveImport.first() }
        val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
        val retriever = MediaMetadataRetriever()
        val list = ArrayList<MediaItem>()
        val playlist = ArrayList<String>()

        try {
            val files = pickedDir?.listFiles() ?: return

//            val customArtwork =
            MusicServiceConnection.customArtwork =
                findFolderArtwork(files)

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
                    list.add(mediaItem)
                    playlist.add(
                        (mediaItem.mediaMetadata.title ?: mediaItem.mediaMetadata.displayTitle
                        ?: "Unknown") as String
                    )
                } else if (file.isDirectory && recursiveImport) {

                }
            }
        } catch (e: Exception) {
            Log.e(className, "Something went wrong by import: ${e.printStackTrace()}")
        } finally {
            retriever.release()
        }


        viewModelScope.launch(Dispatchers.Main) {
            MusicServiceConnection.addMediaItems(list)
        }

        _playlist.value = playlist.toTypedArray()
    }

    fun findFolderArtwork(files: Array<DocumentFile>): Uri? {
        val standardCover = files.firstOrNull { file ->
            val name = file.name?.lowercase() ?: ""
            name == "cover.jpg" || name == "cover.png" ||
                    name == "folder.jpg" || name == "album.jpg" || name == "artwork.jpg"
        }

        if (standardCover != null) {
            Log.d("FindFolderArtwork", "Found cover (standard name)")
            return standardCover.uri
        }

        val anyImage = files.firstOrNull { file ->
            val name = file.name?.lowercase() ?: ""
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
        }

        Log.d("FindFolderArtwork", "Returning uri: $anyImage")
        return anyImage?.uri
    }

    fun onSliderMove(float: Float) {
        val duration = MusicServiceConnection.getDuration()
        MusicServiceConnection.seekTo((duration * float).toLong())
    }

    override fun onCleared() {
        _playlist.value = emptyArray<String>()
        MusicServiceConnection.stopTracking()
        super.onCleared()
    }

    fun seekToMedia(index: Int) {
        MusicServiceConnection.seekToMediaItem(index)
    }

}