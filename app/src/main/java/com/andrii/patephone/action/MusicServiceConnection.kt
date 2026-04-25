package com.andrii.patephone.action

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.andrii.patephone.UpdatedService
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MusicServiceConnection @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val _progress = MutableStateFlow(0f)
    private val _isPlaying = MutableStateFlow(false)
    private val _isShuffleEnabled = MutableStateFlow(false)
    private val _artist = MutableStateFlow("Artist")
    private val _title = MutableStateFlow("Absolutely nothing")


    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
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
                    _artist.value = mediaItem?.mediaMetadata?.artist?.toString() ?: "Unknown"
                    _title.value = mediaItem?.mediaMetadata?.title?.toString() ?: "Untitled"
                }
            })
        }, MoreExecutors.directExecutor())
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
                delay(1000L)
            }
        }
    }

    fun stopTracking() {
        job?.cancel()
    }

    fun play() = mediaController?.play()
    fun pause() = mediaController?.pause()
    fun skipToNext() = mediaController?.seekToNext()
    fun skipToPrevious() = mediaController?.seekToPrevious()
    fun seekTo(position: Long) = mediaController?.seekTo(position)
    fun addMediaItem(mediaItem: MediaItem){mediaController?.addMediaItem(mediaItem)}
    fun getMediaItemCount(): Int {return mediaController?.mediaItemCount ?: 0}

    fun onDestroy() {
        mediaController?.stop()
        mediaController?.release()
        mediaController = null
    }

    fun getDuration() = mediaController?.duration ?: 0
    fun toggleShuffle () {
        val mode = isShuffleEnabled.value
        mediaController?.shuffleModeEnabled = !mode
        _isShuffleEnabled.value = !mode
    }
        fun toggleRepeat() {
        when (mediaController?.repeatMode){
            Player.REPEAT_MODE_ALL -> mediaController?.repeatMode = Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> mediaController?.repeatMode = Player.REPEAT_MODE_OFF
            else -> mediaController?.repeatMode = Player.REPEAT_MODE_ALL
        }
        _repeatMode.value = mediaController?.repeatMode ?: _repeatMode.value
    }

     fun clearMediaItems() = mediaController?.clearMediaItems()
}