package com.andrii.patephone

import android.app.Notification
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.andrii.patephone.action.MusicServiceConnection
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UpdatedService : MediaSessionService() {
    @Inject
    lateinit var musicServiceConnection: MusicServiceConnection
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val notification = createNotification()
        startForeground(1, notification)

        val player = ExoPlayer.Builder(this).build()
        player.addListener(playerListener)
        mediaSession = MediaSession.Builder(this, player).build()
        Log.d("UpdatedService", "Service started")

        musicServiceConnection.startTracking()
    }

// This method starts intent for activity to update UI
    // Activity updates UI if intent contains extra playerStateChanged = true
    private val playerListener = object: Player.Listener{
        override fun onEvents(player: Player, events: Player.Events) {
            if (mediaSession == null) return
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED ) || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)){
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra("playerStateChanged", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                Log.d("UpdatedService", "Player state changed")
            }
            super.onEvents(player, events)
        }
    }


    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Музыкальный плеер")
            .setContentText("Воспроизведение")
            .setOngoing(true)
            .build()
    }


    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d("UpdatedService", "onGetSession called, returning: ${mediaSession != null}")
        return mediaSession
    }

    override fun onDestroy() {
        try {
            mediaSession?.player?.let {
                it.stop()
                it.clearMediaItems()
            }

            mediaSession?.let {
                it.player.clearMediaItems()
                it.player.release()
                it.release()
            }
            mediaSession = null

        } catch (e: Exception) {
            Log.e("UpdatedService", "Error during onDestroy: ${e.message}")
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        musicServiceConnection.stopTracking()
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }
}
