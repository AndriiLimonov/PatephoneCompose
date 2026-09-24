package com.andrii.patephone.core.data

import android.net.Uri

data class Song(
    val title: String = "Absolutely nothing",
    val artist: String = "Artist",
    val artworkUri: Uri? = null
)
