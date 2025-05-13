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

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

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