package com.andrii.patephone.core.data

sealed class PlaylistItem(open val value: String) {
    data class SongItem(val index: Int, override val value: String) : PlaylistItem(value)
    data class FolderTitle(override val value: String) : PlaylistItem(value)
}