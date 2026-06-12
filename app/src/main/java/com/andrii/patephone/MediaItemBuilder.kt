package com.andrii.patephone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File
import java.io.FileOutputStream

class MediaItemBuilder(val context: Context, val retriever: MediaMetadataRetriever, val customArtwork: Uri?, val useArtwork: Boolean, val mediaID: String) {
    fun build(file: DocumentFile): MediaItem {
        val uri = file.uri
        return try {
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
//            val artwork = tryToCacheArtwork(uri)
            val artwork = null
            val fallbackName = file.name ?: "Unknown"

            MediaItem.Builder()
                .setMediaId(mediaID)
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title ?: fallbackName)
                        .setArtist(artist ?: fallbackName)
//                        .setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_BAND_ORCHESTRA)
                        .setArtworkUri(artwork ?: customArtwork)
                        .build()
                )
                .build()
        } catch (_: Exception) {
            MediaItem.fromUri(uri)
        }
    }

    private fun tryToCacheArtwork(uri: Uri): Uri? {
        val cacheDir = context.cacheDir
        val artworkFile = File(cacheDir, "artwork_$mediaID.jpg")

        if (artworkFile.exists()) {
            return Uri.fromFile(artworkFile)
        }

        if (!useArtwork) return null
        return try {
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null){
                val bitmap = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                FileOutputStream(artworkFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap.recycle()
                Uri.fromFile(artworkFile)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
