package com.andrii.patephone.action

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.andrii.patephone.MediaItemBuilder
import com.andrii.patephone.UpdatedService
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class MusicServiceConnection @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(0f)
    private val _isPlaying = MutableStateFlow(false)
    private val _isShuffleEnabled = MutableStateFlow(false)
    private val _artist = MutableStateFlow("Artist")
    private val _title = MutableStateFlow("Absolutely nothing")
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    private val _currentSongIndex = MutableStateFlow(0)
    private val _artworkUri = MutableStateFlow<Uri?>(null)
    val artworkUri = _artworkUri.asStateFlow()
    val currentSongIndex = _currentSongIndex.asStateFlow()
    val repeatMode = _repeatMode.asStateFlow()
    val title = _title.asStateFlow()
    val artist = _artist.asStateFlow()
    val progress = _progress.asStateFlow()
    val isPlaying = _isPlaying.asStateFlow()
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()

    private var mediaController: MediaController? = null

    private fun startService() {
        val intent = Intent(context, UpdatedService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    init {
        startService()

        val sessionToken = SessionToken(context, ComponentName(context, UpdatedService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            // Listener
            mediaController?.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    _isPlaying.value = player.isPlaying
                    _isShuffleEnabled.value = player.shuffleModeEnabled
                    super.onEvents(player, events)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    Log.d("MusicServiceConnection", "Media item transition")

                    if (mediaItem?.mediaMetadata?.title == null) {
                        lazyEnrichment()
                    }


                    updateUI(mediaController?.currentMediaItem)
                }
            })
        }, MoreExecutors.directExecutor())
    }

    fun lazyEnrichment() {
        val mediaItem = mediaController?.currentMediaItem
        if (mediaItem == null || mediaController == null) return
        val currentIndex = mediaController!!.currentPeriodIndex
        val mediaID = mediaItem.mediaId

        val updatedMediaItem = MediaItemBuilder(
            context = context,
            retriever = MediaMetadataRetriever(),
            customArtwork = mediaItem.mediaMetadata.artworkUri,
            seekArtwork = true,
            mediaID = mediaID
        ).buildUpon(mediaItem)

        mediaController!!.replaceMediaItem(currentIndex, updatedMediaItem)
    }

    private fun updateUI(mediaItem: MediaItem?) {
        _title.value =
            mediaItem?.mediaMetadata?.title?.toString() ?: "Untitled"
        _artist.value =
            mediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown"
        _artworkUri.value =
            mediaItem?.mediaMetadata?.artworkUri
        _currentSongIndex.value =
            mediaController?.currentMediaItemIndex ?: 0
    }

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startTracking() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                val pos = mediaController?.currentPosition?.toFloat() ?: 0f
                val dur = mediaController?.duration?.toFloat() ?: 1f
                _progress.value = pos / dur
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
        mediaController?.seekTo(index, 0)
    }

    fun addMediaItems(arr: ArrayList<MediaItem>) {
        mediaController?.addMediaItems(arr)
        Log.d(this::class.simpleName, "media items added: ${arr.size}")
    }

    fun stopTracking() {
        job?.cancel()
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
        _progress.value = 0f
        _isPlaying.value = false
        _title.value = "Absolutely nothing"
        _artist.value = "Artist"
    }

    suspend fun clearArtworkCache() = withContext(Dispatchers.IO) {
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

    fun getDuration() = mediaController?.duration ?: 0
    fun toggleShuffle() {
        val mode = isShuffleEnabled.value
        mediaController?.shuffleModeEnabled = !mode
        _isShuffleEnabled.value = !mode
    }

    fun toggleRepeat() {
        if (mediaController == null) return
        when (mediaController?.repeatMode) {
            Player.REPEAT_MODE_ALL -> mediaController?.repeatMode = Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> mediaController?.repeatMode = Player.REPEAT_MODE_OFF
            else -> mediaController?.repeatMode = Player.REPEAT_MODE_ALL
        }
        _repeatMode.value = mediaController!!.repeatMode
    }

    fun clearMediaItems() = mediaController?.clearMediaItems()
}