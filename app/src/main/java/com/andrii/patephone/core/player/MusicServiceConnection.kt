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
import com.andrii.patephone.core.data.PlayerState
import com.andrii.patephone.core.data.Song
import com.andrii.patephone.features.settings.SettingsManager
import com.andrii.patephone.features.settings.SettingsManager.NotificationHeader
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

object MusicServiceConnection {
    val fallbackPictures by lazy { mutableListOf<DocumentFile>() }
    val UNSUPPORTED_TYPES by lazy { arrayOf("m3u") }
    val className: String = this::class.java.simpleName
    var customArtwork: Uri? = null
    private val _song = MutableStateFlow(Song())
    private val _playerState = MutableStateFlow(PlayerState())
    val song = _song.asStateFlow()
    val playerState = _playerState.asStateFlow()
    private var controllerStarted = false
    private var mediaController: MediaController? = null

    private fun startService(context: Context) {
        val intent = Intent(context, SessionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun getIsControllerStarted() = controllerStarted

    fun clearArtworks() {
        customArtwork = null
        fallbackPictures.clear()
    }

    private val controllerStartedDeferred = CompletableDeferred<Unit>()

    fun onControllerStarted() {
        controllerStartedDeferred.complete(Unit)
        controllerStarted = true
    }

    suspend fun handleItem(item: MediaItem) {
        controllerStartedDeferred.await()

        addMediaItem(item)
        Log.d(
            className,
            "Intent handled! ${item.mediaId}"
        )
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
        updateController(context)

        Log.d(
            className,
            "Init complete!"
        )
    }

    fun shuffleAll() {
        if (mediaController == null || mediaController?.mediaItemCount == 0) return
        val randomIndex = Random.nextInt(mediaController!!.mediaItemCount)
        seekToMediaItem(randomIndex)
        mediaController?.shuffleModeEnabled = true
        play()
    }

    fun updateController(context: Context) {
        val appContext = context.applicationContext
        val sessionToken =
            SessionToken(appContext, ComponentName(appContext, SessionService::class.java))
        val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            controllerStartedDeferred.complete(Unit)
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

                    if (mediaItem?.mediaMetadata?.extras?.getBoolean(
                            "is_enriched",
                            false
                        ) != true
                    ) {
                        Log.d(className, "Enriching media item...")
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
        _playerState.value = oldState.copy(
            currentSongIndex = mediaController?.currentMediaItemIndex ?: 0
        )
    }

    fun toggleShowTimeAboveSlider(value: Boolean) {
        val state = _playerState.value
        _playerState.value = state.copy(showTimeAboveSlider = value)
    }

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startTracking() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                val pos = mediaController?.currentPosition ?: 0
                val dur = mediaController?.duration ?: 1
                _playerState.value =
                    _playerState.value.copy(currentSongDuration = dur, currentTime = pos)
                delay(1000L.milliseconds)
            }
        }
    }

    fun play() = mediaController?.play()
    fun pause() = mediaController?.pause()
    fun stop() = mediaController?.stop()
    fun skipToNext() = mediaController?.seekToNext()
    fun skipToPrevious() = mediaController?.seekToPrevious()
    fun seekToMediaItem(index: Int) = mediaController?.seekTo(index, 0)
    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        // Отправляем новые данные о позиции в активити
        val pos = mediaController?.currentPosition ?: 0
        val dur = mediaController?.duration ?: 1
        _playerState.value =
            _playerState.value.copy(currentSongDuration = dur, currentTime = pos)
    }

    fun addMediaItem(mediaItem: MediaItem) {
        mediaController?.addMediaItem(mediaItem) ?: Log.d(
            className,
            "Adding mediaItem while mediaController is null"
        )
    }

    fun getMediaItemCount(): Int {
        return mediaController?.mediaItemCount ?: 0
    }

    fun addMediaItems(arr: ArrayList<MediaItem>) {
        mediaController?.setMediaItems(arr, false)
        Log.d(this::class.simpleName, "media items added: ${arr.size}")
    }

    fun stopTracking() {
        job?.cancel()
    }

    fun getCurrentMediaItem(): MediaItem? {
        return mediaController?.currentMediaItem
    }

    fun onDestroy() {
        Log.d(
            className,
            "onDestroy() started"
        )
        job?.cancel()
        scope.cancel()
        returnToDefaults()

        mediaController?.release()
        mediaController = null
        Log.d(className, "onDestroy() finished")
    }

    private fun returnToDefaults() {
        _playerState.value = PlayerState(
            currentSongIndex = 0
        )
    }

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
