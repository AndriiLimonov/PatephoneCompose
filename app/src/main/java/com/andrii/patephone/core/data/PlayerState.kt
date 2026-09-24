package com.andrii.patephone.core.data

import androidx.media3.common.Player

data class PlayerState(
    val progress: Float = 0f,
    val isPlaying: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val currentSongIndex: Int
)
