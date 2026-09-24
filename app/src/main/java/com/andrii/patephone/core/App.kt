package com.andrii.patephone.core

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.andrii.patephone.core.player.MusicServiceConnection

const val NOTIFICATION_CHANNEL_ID = "Patephone_music_channel"
const val TAG_VIEW_MODEL = "MainViewModel"

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Creating notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Patephone Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        MusicServiceConnection.init(this)
    }
}
