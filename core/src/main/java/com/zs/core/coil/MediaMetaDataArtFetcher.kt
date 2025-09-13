/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.core.coil

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build.VERSION.SDK_INT
import android.provider.MediaStore
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.DecodeUtils
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.Dimension
import coil3.toAndroidUri

private const val TAG = "MediaMetaDataArtFetcher"

/** [MediaMetadataRetriever] doesn't implement [AutoCloseable] until API 29. */
private inline fun <T> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> T): T {
    try {
        return block(this)
    } finally {
        // We must call 'close' on API 29+ to avoid a strict mode warning.
        if (SDK_INT >= 29) close() else release()
    }
}

/**
 * This class is responsible for fetching real album art from media using the [MediaMetadataRetriever].
 *
 * It should be used with caution, as it consumes significant system resources. Ensure that it is only called
 * with the user's explicit consent.
 *
 * The [data] property in this class represents the URI of the album art. To minimize project-wide changes,
 * we use this property as-is. Internally, it retrieves the actual data column from the MediaStore audio database
 * using the [MediaMetadataRetriever] to fetch the artwork. If the artwork is null, it returns a default art
 * instead of null. This approach is taken to prevent Coil (an image loading library) from loading artwork
 * from its built-in Content URI component, which may lead to incorrect artwork retrieval from the MediaStore.
 *
 * @param data The URI representing the album art. Use with caution and ensure user consent.
 * @param options The options for fetching the album art.
 */
class MediaMetaDataArtFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {
    /**
     * Creates a [Fetcher] for the given [Uri] if it is an album art URI.
     */
    class Factory : Fetcher.Factory<Uri> {
        override fun create(uri: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Check if the URI is a content URI (used for content providers like MediaStore).
            // Check if the URI authority is MediaStore (indicates it's a media file).
            // Example URI: content://media/external/audio/albumart/1961323289806133467
            val data = uri.toAndroidUri()
            if (data.authority != MediaStore.AUTHORITY || data.scheme != ContentResolver.SCHEME_CONTENT) return null
            // Get the path segments of the URI.
            val segments = data.pathSegments
            val size = segments.size
            // Check if the URI points to an album art image in MediaStore.
            // If it's an album art URI, create a MediaMetaDataArtFetcher to handle it.
            // Otherwise, return null (no fetcher available for this URI).
            return if (size >= 3 && segments[size - 3] == "audio" && segments[size - 2] == "albumart")
                MediaMetaDataArtFetcher(uri, options)
            else null
        }
    }

    override suspend fun fetch(): FetchResult? {
        // The 'data[uri]' we receive above still represents the album art URI.
        // However, this URI format is not directly compatible with the MediaMetadataRetriever.
        // To work with MediaMetadataRetriever, we need to extract the album ID from this URI.
        // Once we have the album ID, we can query the MediaStore for the '_data' column
        // associated with that album ID. This should give us the unique '_data' field
        // pointing to the embedded image data.
        // We can then initialize the MediaMetadataRetriever and use it to extract the embedded
        // image data from the '_data' column of the audio record associated with this album art URI.
        // Why this? Because we do not want to make any changes to the project, hence this approach
        // allows us to work with the existing structure without significant modifications.
        val resolver = options.context.contentResolver
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} == ${ContentUris.parseId(data.toAndroidUri())}"
        val parent = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // Query the data using the resolver with specified parameters.
        val data = resolver.query(parent, projection, selection, null, null).use {
            // This section of code handles an unexpected scenario where the retrieval of album art data
            // encounters an issue. If this situation occurs, we should return the default artwork instead.
            // This is a fallback mechanism to ensure that even in the face of errors or missing data,
            // we can provide a default result.
            if (it == null || !it.moveToFirst())
                return null
            // At this point in the code, we expect that the path should not be null.
            // However, if somehow it is null, we again fall back to the default result.
            it.getString(0) ?: return null
        }
        // Initialize a MediaMetadataRetriever to work with the retrieved data.
        return MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(data)
            // Extract the embedded artwork from the MediaMetadataRetriever.
            // or else throw exception to stop from retiving it.
            val bytes = retriever.embeddedPicture ?: throw IllegalStateException("No embedded artwork found")
            var isSampled: Boolean
            val bitmap = BitmapFactory.Options().let {
                // Set inJustDecodeBounds to true to determine the image dimensions without loading it.
                it.inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, it)

                // Initialize the destination size; fallback to the default size if null.
                val width = (options.size.width as? Dimension.Pixels)?.px ?: it.outWidth
                val height = (options.size.height as? Dimension.Pixels)?.px ?: it.outHeight

                // Determine if the image needs to be sampled based on its original dimensions.
                isSampled = if (it.outWidth > 0 && it.outHeight > 0) {

                    // Calculate the size multiplier and check if it's less than 1.0.
                    // If it's less than 1.0, consider the image as sampled.
                    DecodeUtils.computeSizeMultiplier(
                        srcWidth = it.outWidth,
                        srcHeight = it.outHeight,
                        dstWidth = width,
                        dstHeight = height,
                        scale = options.scale
                    ) < 1.0
                } else {

                    // We were unable to determine the original size of the image.
                    // Assume it is sampled to avoid potential issues.
                    true
                }
                it.inSampleSize = DecodeUtils.calculateInSampleSize(
                    it.outWidth,
                    it.outHeight,
                    width,
                    height,
                    options.scale
                )

                // Calculate the inSampleSize for bitmap decoding.
                it.inJustDecodeBounds = false

                // Decode the bitmap from the provided byte array.
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, it)
            }

            ImageFetchResult(
                image = bitmap.asImage(),
                isSampled = isSampled,
                dataSource = DataSource.DISK
            )
        }
    }
}