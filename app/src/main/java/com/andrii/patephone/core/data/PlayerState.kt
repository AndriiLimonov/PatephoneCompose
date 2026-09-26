package com.andrii.patephone.core.data

import androidx.media3.common.Player

data class PlayerState(
    val isPlaying: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val currentSongIndex: Int = 0,
    val currentSongDuration: Long = 0, // Time in milliseconds
    val currentTime: Long = 0
)
