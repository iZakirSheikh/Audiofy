package com.prime.media.old.core.playback

import android.content.Context
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.annotation.OptIn
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.*
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.prime.media.R
import java.io.File
import java.io.FileOutputStream

private const val TAG = "Delegates"

private const val MEDIA_ITEM_EXTRA_MIME_TYPE = "media_item_extra_mime_type"

/**
 * Constructs a media source from the provided parameters.
 * This constructs the media source that is playable by the media service's exoplayer.
 *
 * @param uri The URI of the media source.
 * @param title The title of the media source.
 * @param subtitle The subtitle of the media source.
 * @param artwork The artwork URI of the media source.
 * @return A media source that is playable by the media service's exoplayer.
 */
fun MediaSource(
    uri: Uri,
    title: CharSequence,
    subtitle: CharSequence,
    artwork: Uri? = null,
) = MediaItem.Builder()
    .setMediaId("no_empty")
    .setUri(uri)
    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
    .setMediaMetadata(
        Builder().setIsBrowsable(false)
            .setIsPlayable(true)
            .setTitle(title)
            .setArtist(subtitle)
            .setArtworkUri(artwork)
            .setSubtitle(subtitle)
            .build()
    )
    .build()

/**
 * Returns a spannable string representation of [value]
 */
private fun Bold(value: CharSequence): CharSequence =
    SpannableStringBuilder(value).apply {
        setSpan(StyleSpan(Typeface.BOLD), 0, value.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }


/**
 * returns the positions array from the [DefaultShuffleOrder]
 *
 * FixMe: Extracts array from player using reflection.
 */
val Player.orders: IntArray
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    get() {
        require(this is ExoPlayer)
        val f1 = this.javaClass.getDeclaredField("shuffleOrder")
        f1.isAccessible = true
        val order2 = f1.get(this)
        require(order2 is DefaultShuffleOrder)
        val f2 = order2.javaClass.getDeclaredField("shuffled")
        f2.isAccessible = true
        return f2.get(order2) as IntArray
    }


/**
 * Returns all the [MediaItem]s of [Player] in their natural order.
 *
 * @return A list of [MediaItem]s in the player's natural order.
 */
inline val Player.mediaItems
    get() = List(this.mediaItemCount) {
        getMediaItemAt(it)
    }

/**
 * The queue property represents the list of media items in the player's queue.
 * If shuffle mode is not enabled, the queue will contain the media items in their natural order.
 * If shuffle mode is enabled, the queue will contain the media items in the order specified by the 'orders' list.
 *
 * @return The list of media items in the player's queue.
 */
val Player.queue get() = if (!shuffleModeEnabled) mediaItems else orders.map(::getMediaItemAt)

/**
 * Creates a new [MediaItem] instance using the provided parameters.
 *
 * @param uri The URI of the media item.
 * @param title The title of the media item.
 * @param subtitle The subtitle of the media item.
 * @param id The unique identifier of the media item. Defaults to [MediaItem.DEFAULT_MEDIA_ID].
 * @param artwork The URI of the artwork for the media item. Defaults to null.
 * @return The new [MediaItem] instance.
 *
 */
@Deprecated("Use the one from com.prime.media.common")
fun MediaItem(
    uri: Uri,
    title: String,
    subtitle: String,
    id: String = "non_empty",
    artwork: Uri? = null,
    mimeType: String? = null
) = MediaItem.Builder()
    .setMediaId(id)
    .setMimeType(mimeType)
    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
    .setMediaMetadata(
        Builder()
            .setArtworkUri(artwork)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(Bundle().apply { putString(MEDIA_ITEM_EXTRA_MIME_TYPE, mimeType) })
            // .setExtras(bundleOf(ARTIST_ID to artistId, ALBUM_ID to albumId))
            .build()
    ).build()

@Deprecated("Needs to be replaced with something new.")
val MediaItem.artworkUri get() = mediaMetadata.artworkUri
val MediaItem.title get() = mediaMetadata.title
val MediaItem.subtitle get() = mediaMetadata.subtitle
val MediaItem.mediaUri get() = requestMetadata.mediaUri

/**
 * A function that returns the file name from a given URI
 * @param context The context object to access the content resolver
 * @param uri The URI of the file
 * @return The file name as a string, or null if not found
 */
private fun Context.fileName(uri: Uri): String? {
    // Use DocumentFile to get the display name
    val displayName = DocumentFile.fromSingleUri(this, uri)?.name
    // Return the display name if not null
    if (displayName != null) {
        return displayName
    }
    // Use ContentResolver to query the content provider
    val cursor = this.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        // Move to the first row
        if (it.moveToFirst()) {
            // Try to get the display name column
            val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex != -1) {
                // Return the display name value
                return it.getString(displayNameIndex)
            }
            // Try to get the data column
            val dataIndex = it.getColumnIndex(MediaStore.MediaColumns.DATA)
            if (dataIndex != -1) {
                // Return the last segment of the data value
                return it.getString(dataIndex).substringAfterLast('/')
            }
        }
    }
    // Use File to get the path and name
    val fileName = File(uri.path ?: return null).name
    // Return the file name if not empty
    if (fileName.isNotEmpty()) {
        return fileName
    }
    // Return null if no file name found
    return null
}

/**
 * Constructs a new [MediaItem] from the provided [uri].
 *
 * This factory method creates a [MediaItem] object from the given URI by extracting
 * media metadata such as title, subtitle, and artwork. It provides a more convenient
 * and flexible way to create [MediaItem] instances compared to [MediaItem.fromUri],
 * as it allows customization of metadata retrieval and caching of artwork.
 *
 * @param uri The URI of the media item.
 * @return A [MediaItem] object representing the media item.
 */
@Deprecated("Use the one from com.prime.media.common")
fun MediaItem(context: Context, uri: Uri, mimeType: String?): MediaItem {
    // Create a MediaMetadataRetriever object and set the data source.
    // maybe it might cause crash; android is stupid.
    val retriever = com.primex.core.runCatching(TAG) {
        MediaMetadataRetriever().also { it.setDataSource(context, uri) }
    }

    // Get the URI of the embedded image and cache it.
    val imageUri = com.primex.core.runCatching(TAG) {
        val file = File(context.cacheDir, "tmp_artwork.png")
        // Delete the old cached artwork file, if it exists.
        // This ensures that the latest album artwork is used, even if the track previously lacked artwork.
        file.delete()
        val bytes = retriever?.embeddedPicture ?: return@runCatching null
        val fos = FileOutputStream(file)
        fos.write(bytes)
        fos.close()
        Uri.fromFile(file)
    }
    // Obtain title and subtitle
    val title = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        ?: context.fileName(uri) ?: context.getString(R.string.unknown)
    val subtitle = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        ?: context.getString(R.string.unknown)
    // Construct a MediaItem using the obtained parameters.
    // (Currently, details about playback queue setup are missing.)
    return MediaItem(uri, title, subtitle, artwork = imageUri, mimeType = mimeType)
}

