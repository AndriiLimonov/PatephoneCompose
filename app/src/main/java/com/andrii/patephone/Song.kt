package com.andrii.patephone

import android.net.Uri
import androidx.media3.common.Player

data class Song(
    val title: String = "Absolutely nothing",
    val artist: String = "Artist",
    val artworkUri: Uri? = null
)

data class PlayerState (
    val progress: Float = 0f,
    val isPlaying: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val currentSongIndex: Int
)
/*
    private val _progress = MutableStateFlow(0f)
    private val _isPlaying = MutableStateFlow(false)
    private val _isShuffleEnabled = MutableStateFlow(false)
    private val _artist = MutableStateFlow("Artist")
    private val _title = MutableStateFlow("Absolutely nothing")
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    private val _currentSongIndex = MutableStateFlow(0)
    private val _artworkUri = MutableStateFlow<Uri?>(null)
*/