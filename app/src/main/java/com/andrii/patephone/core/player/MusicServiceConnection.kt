package com.andrii.patephone.core.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.andrii.patephone.core.player.MediaItemBuilder
import com.andrii.patephone.core.data.PlayerState
import com.andrii.patephone.features.settings.SettingsManager
import com.andrii.patephone.core.data.Song
import com.andrii.patephone.core.player.UpdatedService
import com.andrii.patephone.features.settings.SettingsManager.NotificationHeader
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

object MusicServiceConnection {
    val fallbackPictures = mutableListOf<DocumentFile>()
    val className: String = this::class.java.simpleName
    val UNSUPPORTED_TYPES = arrayOf("m3u")
    var customArtwork: Uri? = null
    private val _song = MutableStateFlow(Song())
    private val _playerState = MutableStateFlow(
        PlayerState(
            currentSongIndex = 0
        )
    )
    val song = _song.asStateFlow()
    val playerState = _playerState.asStateFlow()

    private var mediaController: MediaController? = null

    private fun startService(context: Context) {
        val intent = Intent(context, UpdatedService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private suspend fun getNotificationHeader(context: Context): NotificationHeader {
        val property =
            SettingsManager(context).notificationHeader.first()
        return property.also { Log.d(className, "NotificationHeader = $it") }
    }

    private suspend fun getUseMetadataArtwork(context: Context): Boolean {
        val property =
            SettingsManager(context).useMetadataArtwork.first()
        return property.also { Log.d(className, "UseMetadataArtwork = $it") }
    }

    fun init(context: Context) {
        val appContext = context.applicationContext
        startService(appContext)

        val sessionToken =
            SessionToken(appContext, ComponentName(appContext, UpdatedService::class.java))
        val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            // Listener
            mediaController?.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = player.isPlaying,
                        isShuffleEnabled = player.shuffleModeEnabled
                    )
                    super.onEvents(player, events)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    Log.d("MusicServiceConnection", "Media item transition")

                    if (mediaItem?.mediaMetadata?.title == null) {
                        lazyEnrichment(appContext)
                    }


                    updateUI(mediaController?.currentMediaItem)
                }
            })
        }, MoreExecutors.directExecutor())
    }

    fun lazyEnrichment(context: Context) {
        val appContext = context.applicationContext
        val mediaItem = mediaController?.currentMediaItem
        if (mediaItem == null || mediaController == null) return
        val currentIndex = mediaController!!.currentPeriodIndex
        val mediaID = mediaItem.mediaId
        val retriever = MediaMetadataRetriever()

        val updatedMediaItem = MediaItemBuilder(
            context = appContext,
            retriever = retriever,
            customArtwork = customArtwork,
            seekArtwork = runBlocking { getUseMetadataArtwork(appContext) },
            mediaID = mediaID,
            notificationHeader = runBlocking { getNotificationHeader(appContext) }
        ).buildUpon(mediaItem)

        retriever.release()
        mediaController!!.replaceMediaItem(currentIndex, updatedMediaItem)
    }


    private fun updateUI(mediaItem: MediaItem?) {
        _song.value = Song(
            title = mediaItem?.mediaMetadata?.title?.toString() ?: "Untitled",
            artist = mediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown",
            artworkUri = mediaItem?.mediaMetadata?.artworkUri
        )
        val oldState = _playerState.value
        _playerState.value = PlayerState(
            progress = oldState.progress,
            isPlaying = oldState.isPlaying,
            isShuffleEnabled = oldState.isShuffleEnabled,
            repeatMode = oldState.repeatMode,
            currentSongIndex = mediaController?.currentMediaItemIndex ?: 0
        )
        /*
        _title.value =
            mediaItem?.mediaMetadata?.title?.toString() ?: "Untitled"
        _artist.value =
            mediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown"
        _artworkUri.value =
            mediaItem?.mediaMetadata?.artworkUri
        _currentSongIndex.value =
            mediaController?.currentMediaItemIndex ?: 0
         */
    }

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startTracking() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                val pos = mediaController?.currentPosition?.toFloat() ?: 0f
                val dur = mediaController?.duration?.toFloat() ?: 1f
                _playerState.value = _playerState.value.copy(progress = pos / dur)
                delay(1000L.milliseconds)
            }
        }
    }

    fun play() = mediaController?.play()
    fun pause() = mediaController?.pause()
    fun stop() = mediaController?.stop()
    fun skipToNext() = mediaController?.seekToNext()
    fun skipToPrevious() = mediaController?.seekToPrevious()
    fun seekTo(position: Long) = mediaController?.seekTo(position)

    fun addMediaItem(mediaItem: MediaItem) {
        mediaController?.addMediaItem(mediaItem)
    }

    fun getMediaItemCount(): Int {
        return mediaController?.mediaItemCount ?: 0
    }

    fun seekToMediaItem(index: Int) {
        // We just toggle shuffle off and back if it was on to trigger onShuffleModeChanged listener placed in UpdatedService: 69
        if (mediaController?.shuffleModeEnabled == true) {
            mediaController?.shuffleModeEnabled = false
            mediaController?.shuffleModeEnabled = true
        }
        mediaController?.seekTo(index, 0)
    }

    fun addMediaItems(arr: ArrayList<MediaItem>) {
        mediaController?.addMediaItems(arr)
        Log.d(this::class.simpleName, "media items added: ${arr.size}")
    }

    fun stopTracking() {
        job?.cancel()
    }

    fun getCurrentMediaItem(): MediaItem? {
        return mediaController?.currentMediaItem
    }

    fun onDestroy() {
        job?.cancel()
        scope.cancel()
        returnToDefaults()

        mediaController?.release()
        mediaController = null
        Log.d("MusicServiceConnection", "Singleton manager destroyed")
    }

    private fun returnToDefaults() {
        _playerState.value = PlayerState(
            currentSongIndex = 0
        )
    }

    /* suspend */ fun clearArtworkCache(): Nothing = TODO()

    /*
withContext(Dispatchers.IO) {

    Log.d("MusicServiceConnection", "Clearing cached artworks...")
    try {
        val cacheDir = context.cacheDir
        val artworkFiles = cacheDir.listFiles { _, name ->
            name.startsWith("artwork_") && name.endsWith(".jpg")
        }

        artworkFiles?.forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    Log.d("MusicServiceConnection", "Artworks cleaned!")
}
*/
    fun getDuration() = mediaController?.duration ?: 0
    fun toggleShuffle() {
        val mode = playerState.value.isShuffleEnabled
        mediaController?.shuffleModeEnabled = !mode
        _playerState.value = _playerState.value.copy(isShuffleEnabled = !mode)
    }

    fun toggleRepeat() {
        if (mediaController == null) return
        when (mediaController?.repeatMode) {
            Player.REPEAT_MODE_ALL -> mediaController?.repeatMode = Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> mediaController?.repeatMode = Player.REPEAT_MODE_OFF
            else -> mediaController?.repeatMode = Player.REPEAT_MODE_ALL
        }
        _playerState.value =
            _playerState.value.copy(repeatMode = mediaController!!.repeatMode)
    }

    fun clearMediaItems() = mediaController?.clearMediaItems()
}
