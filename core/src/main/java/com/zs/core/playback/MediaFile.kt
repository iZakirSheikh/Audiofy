/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.core.playback

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.zs.core.db.playlists.Playlist

/**
 * Constant for the MIME type extra key in media items.
 */
private const val MEDIA_ITEM_EXTRA_MIME_TYPE = "media_item_extra_mime_type"

/**
 * Gets the artwork URI of a [MediaItem].
 */
internal val MediaItem.artworkUri get() = mediaMetadata.artworkUri

/**
 * Gets the title of a [MediaItem].
 */
internal val MediaItem.title get() = mediaMetadata.title

/**
 * Gets the subtitle of a [MediaItem].
 */
internal val MediaItem.subtitle get() = mediaMetadata.subtitle

/**
 * Gets the URI of a [MediaItem].
 */
internal val MediaItem.mediaUri get() = requestMetadata.mediaUri

/**
 * Gets the MIME type of a [MediaItem].
 */
internal val MediaItem.mimeType get() = mediaMetadata.extras?.get(MEDIA_ITEM_EXTRA_MIME_TYPE) as? String

/**
 * Creates a playable [MediaItem]
 * @see MediaSource
 */
@JvmInline
value class MediaFile internal constructor(internal val value: MediaItem) {
    constructor(
        uri: Uri,
        title: String = "",
        subtitle: String = "",
        artwork: Uri? = null,
        mimeType: String? = null,
    ) : this(
        MediaItem.Builder()
            .setMediaId("no-media-id")
            .setMimeType(mimeType)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setArtworkUri(artwork)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setExtras(Bundle().apply { putString(MEDIA_ITEM_EXTRA_MIME_TYPE, mimeType) })
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            ).build()
    )

    val mediaUri get() = value.mediaUri
    val title get() = value.title?.toString()
    val subtitle get() = value.subtitle?.toString()
    val artworkUri get() = value.artworkUri
    val mimeType get() = value.mimeType
}


/**
 * Represents a [MediaItem] that is a playable.
 *
 * This media source includes its [MediaItem.localConfiguration] and other necessary configurations
 * fully set up.
 *
 * When passing a [MediaItem] to the Player controller, these configurations might get
 * deleted, requiring them to be rebuilt. This function ensures that the [MediaItem] is
 * properly constructed to maintain safety and functionality.
 *
 * @param uri The URI of the media source.
 * @param title The title of the media source.
 * @param subtitle The subtitle of the media source.
 * @param artwork The artwork URI of the media source (optional).
 * @param mimeType The MIME type of the media source (optional).
 * @return A [MediaItem] that is playable by the [Playback]'s ExoPlayer.
 */
internal fun MediaSource(
    uri: Uri,
    title: CharSequence,
    subtitle: CharSequence,
    artwork: Uri? = null,
    mimeType: String? = null,
) = MediaItem.Builder()
    .setMediaId("no-media-id")
    .setUri(uri)
    .setMimeType(mimeType)
    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
    .setMediaMetadata(
        MediaMetadata.Builder().setIsBrowsable(false)
            .setIsPlayable(true)
            .setTitle(title)
            .setArtist(subtitle)
            .setExtras(Bundle().apply { putString(MEDIA_ITEM_EXTRA_MIME_TYPE, mimeType) })
            .setArtworkUri(artwork)
            .setSubtitle(subtitle)
            .build()
    )
    .build()

/**
 * Creates a [MediaItem] representing a browsable media root.
 *
 * This item is not playable and is used to define the type of items requested by the user,
 * such as playlists ([Playback.PLAYLIST_QUEUE], [Playback.PLAYLIST_RECENT]).
 *
 * @param value The media ID for the root item.
 * @return A [MediaItem] representing the browsable media root.
 */
internal fun MediaRoot(value: String): MediaItem =
    MediaItem.Builder()
        .setMediaId(value)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setIsBrowsable(true)
                .setIsPlayable(false)
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
 * Composes and returns a [MediaSource] from this [MediaItem].
 *
 * Note: This [MediaItem] might be or might not be the actual [MediaSource].
 *
 * @return The [MediaSource] extracted from this [MediaItem].
 * @see MediaSource
 */
internal val MediaItem.asMediaSource
    get() = MediaSource(
        uri = mediaUri!!,
        title = Bold(title ?: MediaStore.UNKNOWN_STRING),
        subtitle = subtitle ?: MediaStore.UNKNOWN_STRING,
        artwork = artworkUri,
        mimeType = mimeType
    )

/**
 * @see MediaSource
 */
internal val Playlist.Track.toMediaSource
    get() = MediaSource(
        uri = Uri.parse(uri),
        title = Bold(title),
        subtitle = subtitle,
        artwork = artwork?.let { Uri.parse(it) },
        mimeType = mimeType
    )

/**
 * Factory function that creates a `Member` object with the given `MediaItem` object and some additional metadata.
 *
 * @param from The `MediaItem` object to create the `Member` from.
 * @param playlistId The ID of the playlist to which this `Member` belongs.
 * @param order The order of this `Member` within the playlist.
 * @return A new `Member` object with the given parameters.
 */
internal fun MediaItem.toTrack(playlistId: Long, order: Int) =
    Playlist.Track(
        playlistID = playlistId,
        order = order,
        uri = requestMetadata.mediaUri!!.toString(),
        title = mediaMetadata.title.toString(),
        subtitle = mediaMetadata.subtitle.toString(),
        artwork = mediaMetadata.artworkUri?.toString(),
        mimeType = mimeType
    )

