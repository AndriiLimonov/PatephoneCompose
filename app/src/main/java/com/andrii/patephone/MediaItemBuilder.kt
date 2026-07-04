package com.andrii.patephone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.File
import java.io.FileOutputStream

class MediaItemBuilder(
    val context: Context,
    val retriever: MediaMetadataRetriever?,
    val customArtwork: Uri?,
    val seekArtwork: Boolean,
    val mediaID: String
) {
    // Secondary constructor for freshBuild()
    constructor(
        context: Context,
        customArtwork: Uri?,
        mediaID: String
    ) : this(context, null, customArtwork, false, mediaID)

    // Secondary constructor for freshBuild() without artwork
    constructor(
        context: Context,
        mediaID: String
    ): this(context, null, null, false, mediaID)

    // Build mediaItem without metadata
    fun freshBuild(file: DocumentFile): MediaItem {
        val uri = file.uri
        val title = file.name ?: "unknown"

        return MediaItem.Builder()
            .setMediaId(mediaID)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setDisplayTitle(title)
                    .setArtworkUri(customArtwork)
                    .build()
            )
            .build()
    }

    fun build(file: DocumentFile): MediaItem {
        if (retriever == null) throw NullPointerException("Do not use build() if retriever is null")
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

    private fun tryToCacheArtwork(): Uri? {
        if (retriever == null) throw NullPointerException("Do not use TryToCacheArtwork() if retriever is null")
        val cacheDir = context.cacheDir
        val artworkFile = File(cacheDir, "artwork_$mediaID.jpg")

        if (artworkFile.exists()) {
            return Uri.fromFile(artworkFile)
        }

        if (!seekArtwork) return null
        return try {
            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null) {
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

    fun buildUpon(mediaItem: MediaItem): MediaItem {
        if (retriever == null) throw NullPointerException("Do not use buildUpon() if retriever == null")
        return try {
            Log.d("MediaItemBuilder", "buildUpon started")
            retriever.setDataSource(context, mediaItem.localConfiguration?.uri)
            Log.d("MediaItemBuilder", "uri found")

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val artwork = tryToCacheArtwork()

            Log.d("MediaItemBuilder", "$title, $artwork, $artist")
            mediaItem.buildUpon()
                .setMediaMetadata(
                    mediaItem.mediaMetadata.buildUpon()
                        .setTitle(title)
                        .setArtist(artist)
                        .setArtworkUri(artwork ?: customArtwork)
                        .build()
                )
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("MediaItemBuilder", "Got exception: $e, applying current mediaItem")
            mediaItem
        } finally {
            Log.d("MediaItemBuilder", "built successfully")
        }
    }
}
