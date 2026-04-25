package com.andrii.patephone

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

class MediaItemBuilder(val context: Context, val retriever: MediaMetadataRetriever) {
    fun build(uri: Uri): MediaItem {
        return try {
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

            val fallbackName = uri.lastPathSegment ?: "Unknown"

            MediaItem.Builder()
                .setMediaId(uri.toString())
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title ?: fallbackName)
                        .setArtist(artist ?: fallbackName)
                        .build()
                )
                .build()
        } catch (e: Exception) {
            MediaItem.fromUri(uri)
        }
    }
}
