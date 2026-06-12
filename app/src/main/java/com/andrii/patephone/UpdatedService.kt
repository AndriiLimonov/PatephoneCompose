package com.andrii.patephone

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
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

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val notification = createNotification()
        startForeground(1, notification)

        val renderersFactory = DefaultRenderersFactory(this).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
        }


        val player = ExoPlayer.Builder(this, renderersFactory)
            // Buffering
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        2_500,
                        5_000,
                        500,
                        1_000
                    )
                    .setTargetBufferBytes(8 * 1024 * 1024)
                    .setPrioritizeTimeOverSizeThresholds(false)
                    .build()
            )
            .build()
        player.addListener(playerListener)

            // Session activity intent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            // явное указание activity
            .setSessionActivity(pendingIntent)
            .build()

        Log.d("UpdatedService", "Service started")

        musicServiceConnection.startTracking()
    }

    // This method starts intent for activity to update UI
    // Activity updates UI if intent contains extra playerStateChanged = true
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (mediaSession == null) return
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) || events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
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
        Log.d("UpdatedService", "Starting secure onDestroy...")
        try {
            mediaSession?.let { session ->
                val player = session.player

                player.removeListener(playerListener)
                player.playWhenReady = false
                player.stop()
                player.clearMediaItems()

                session.release()

                Handler(Looper.getMainLooper()).post {
                    player.release()
                    Log.d("UpdatedService", "Player resources async released")
                }
            }
            mediaSession = null

            musicServiceConnection.onDestroy()

        } catch (e: Exception) {
            Log.e("UpdatedService", "Error during onDestroy: ${e.message}", e)
        }

        Log.d("UpdatedService", "Service completely destroyed")
        super.onDestroy()
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("Updated Service", "Closing...")
        mediaSession?.player?.stop()

        musicServiceConnection.stopTracking()
        super.onTaskRemoved(rootIntent)
        stopSelf()

        Handler(Looper.getMainLooper()).postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 300)
    }
}
