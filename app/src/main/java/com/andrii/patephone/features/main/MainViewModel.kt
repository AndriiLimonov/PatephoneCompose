package com.andrii.patephone.features.main

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.andrii.patephone.core.player.MediaItemBuilder
import com.andrii.patephone.core.data.PlayerState
import com.andrii.patephone.core.data.Song
import com.andrii.patephone.core.player.MusicServiceConnection
import com.andrii.patephone.core.TAG_VIEW_MODEL
import com.andrii.patephone.core.player.PlayerAction
import com.andrii.patephone.features.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    val className: String = this::class.java.simpleName
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

    @Suppress("REDUNDANT_ELSE_IN_WHEN")
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
            val recursiveImport: Boolean = SettingsManager(context).recursiveImport.first()
            // this method returns pair, where first value is MediaItem arraylist, and second value is it's names typed array
            val files = listAudioFiles(
                treeUri = treeUri,
                context = context,
                recursiveImportDepth = if (recursiveImport) getRecursiveImportDepth(context) else 0,
                currentDepth = 0
            )
            val playlist = files.first
            viewModelScope.launch(Dispatchers.Main) {
                MusicServiceConnection.addMediaItems(playlist)
                _playlist.value = files.second
            }
        }
    }

    private suspend fun getRecursiveImportDepth(context: Context): Int {
        val property =
            SettingsManager(context).recursiveImportDepth.first()
        return property.also { Log.d(className, "Got recursive import property: $it") }
    }

    // this method returns pair, where first value is MediaItem arraylist, and second value is it's names typed array
    fun listAudioFiles(
        treeUri: Uri,
        context: Context,
        recursiveImportDepth: Int,
        currentDepth: Int
    ): Pair<ArrayList<MediaItem>, Array<String>> {
        val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
        val retriever = MediaMetadataRetriever()
        val playlist = ArrayList<MediaItem>()
        val names = ArrayList<String>()

        try {
            val files = pickedDir?.listFiles() ?: return Pair(ArrayList<MediaItem>(), emptyArray())

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
                    playlist.add(mediaItem)
                    names.add(
                        (mediaItem.mediaMetadata.title ?: mediaItem.mediaMetadata.displayTitle
                        ?: "Unknown") as String
                    )
                } else if (file.isDirectory && currentDepth < recursiveImportDepth) {
                    val files = listAudioFiles(
                        file.uri,
                        context,
                        recursiveImportDepth + 1,
                        currentDepth + 1
                    )
                    files.first.forEach { playlist.add(it) }
                    files.second.forEach { names.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(className, "Something went wrong by import: ${e.printStackTrace()}")
        } finally {
            retriever.release()
        }
        return Pair(playlist, names.toTypedArray())
    }

    fun findFolderArtwork(files: Array<DocumentFile>): Uri? {
        val standardCover = files.firstOrNull { file ->
            val name = file.name?.lowercase() ?: ""
            name == "cover.jpg" || name == "cover.png" ||
                    name == "folder.jpg" || name == "album.jpg" || name == "artwork.jpg"
        }

        if (standardCover != null) {
            Log.d("FindFolderArtwork", "Found cover (standard name)")
            MusicServiceConnection.fallbackPictures.add(standardCover)
            return standardCover.uri
        }

        val anyImage = files.firstOrNull { file ->
            val name = file.name?.lowercase() ?: ""
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
        }

        Log.d("FindFolderArtwork", "Returning uri: $anyImage")
        if (anyImage != null) {
            MusicServiceConnection.fallbackPictures.add(anyImage)
        }
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
