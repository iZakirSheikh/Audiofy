package com.prime.media.core.playback

import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.*
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.prime.media.core.db.Playlist

private const val TAG = "Delegates"

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
 * @see MediaSource
 */
val Playlist.Member.toMediaSource
    get() = MediaSource(Uri.parse(uri), subtitle, Bold(title), artwork?.let { Uri.parse(it) })

/**
 * Composes and returns a [MediaSource] from this [MediaItem].
 *
 * Note: This [MediaItem] might be or might not be the actual [MediaSource].
 *
 * @return The [MediaSource] extracted from this [MediaItem].
 * @see MediaSource
 */
val MediaItem.toMediaSource
    get() = MediaSource(
        requestMetadata.mediaUri!!,
        Bold(mediaMetadata.title ?: ""),
        mediaMetadata.subtitle ?: "",
        mediaMetadata.artworkUri
    )

/**
 * returns the positions array from the [DefaultShuffleOrder]
 *
 * FixMe: Extracts array from player using reflection.
 */
val Player. orders: IntArray
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
val Player.queue get() = if (!shuffleModeEnabled) mediaItems else orders.map { getMediaItemAt(it) }

