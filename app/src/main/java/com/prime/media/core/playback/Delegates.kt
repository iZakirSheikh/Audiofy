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
import com.prime.media.core.db.Playlists
import com.prime.media.core.util.Member

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
    get() = MediaSource(Uri.parse(uri), Bold(title), subtitle, artwork?.let { Uri.parse(it) })

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
 * @see MediaSource
 */
fun MediaItem(
    uri: Uri,
    title: String,
    subtitle: String,
    id: String = "non_empty",
    artwork: Uri? = null,
) = MediaItem.Builder()
    .setMediaId(id)
    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
    .setMediaMetadata(
        Builder()
            .setArtworkUri(artwork)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            // .setExtras(bundleOf(ARTIST_ID to artistId, ALBUM_ID to albumId))
            .build()
    ).build()

val MediaItem.artworkUri get() = mediaMetadata.artworkUri
val MediaItem.title get() = mediaMetadata.title
val MediaItem.subtitle get() = mediaMetadata.subtitle
val MediaItem.mediaUri get() = requestMetadata.mediaUri

/**
 * Adds a [MediaItem] to the list of recent items.
 *
 * This extension function is designed to be used within the context of the [Playback] service
 * and should not be accessed externally.
 *
 * @receiver Playback: The playback service to which this function is scoped.
 * @param item The [MediaItem] to be added to the list of recent items.
 * @param limit The maximum number of recent items to retain.
 *
 * @throws IllegalArgumentException if [limit] is less than or equal to zero.
 *
 * @see Playback
 */
context(Playback)
suspend fun Playlists.addToRecent(item: MediaItem, limit: Long) {

    val playlistId =
        get(Playback.PLAYLIST_RECENT)?.id ?: insert(Playlist(name = Playback.PLAYLIST_RECENT))
    // here two cases arise
    // case 1 the member already exists:
    // in this case we just have to update the order and nothing else
    // case 2 the member needs to be inserted.
    // In both cases the playlist's dateModified needs to be updated.
    val playlist = get(playlistId)!!
    update(playlist = playlist.copy(dateModified = System.currentTimeMillis()))

    val member = get(playlistId, item.requestMetadata.mediaUri.toString())

    when (member != null) {
        // exists
        true -> {
            //localDb.members.update(member.copy(order = 0))
            // updating the member doesn't work as expected.
            // localDb.members.delete(member)
            update(member = member.copy(order = 0))
        }

        else -> {
            // delete above member
            delete(playlistId, limit)
            insert(Member(item, playlistId, 0))
        }
    }
}


/**
 * Replaces the current queue with the provided list of [items].
 *
 * This extension function is designed to be used within the context of the [Playback] service
 * and should not be accessed externally.
 *
 * @receiver Playback: The playback service to which this function is scoped.
 * @param items The list of [MediaItem] to replace the current queue with.
 *
 * @see Playback
 */
context(Playback)
suspend fun Playlists.save(items: List<MediaItem>) {
    val id = get(Playback.PLAYLIST_QUEUE)?.id ?: insert(
        Playlist(name = Playback.PLAYLIST_QUEUE)
    )

    // delete all old
    delete(id, 0)
    var order = 0
    val members = items.map { Member(it, id, order++) }
    insert(members)
}

