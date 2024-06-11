package com.prime.media.core.coil

import android.content.ContentUris
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.provider.MediaStore
import androidx.core.graphics.drawable.toDrawable
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import com.prime.media.R
import com.prime.media.core.db.query2

private const val TAG = "MediaMetaDataArtFetcher"

/**
 *  FixMe: Find Proper way to implement this thing.
 *
 * The default fetch result containing a default drawable to be used when there is an error
 * or failure in retrieving the actual image.
 * This is generated lazily.

 */
private var DEFAULT_RESULT: FetchResult? = null

/** [MediaMetadataRetriever] doesn't implement [AutoCloseable] until API 29. */
private inline fun <T> MediaMetadataRetriever.use(block: (MediaMetadataRetriever) -> T): T {
    try { return block(this) } finally {
        // We must call 'close' on API 29+ to avoid a strict mode warning.
        if (SDK_INT >= 29) close() else release()
    }
}

// TODO: Move this class to its proper place.
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
    override suspend fun fetch(): FetchResult {
        // Initialize the default fetcher only once.
        // But why is this required, you might wonder?
        // The necessity arises from the fact that if we simply return the default implementation
        // of UriFetcher provided by Coil, it may return incorrect or invalid artwork for certain results.
        // So, to ensure consistent and reliable default artwork behavior, we set up a DrawableResult
        // representing the default artwork. This DrawableResult is then used as the fallback
        // when fetching artwork for a particular result.
        if (DEFAULT_RESULT == null) {
            DEFAULT_RESULT = DrawableResult(
                options.context.getDrawable(R.drawable.default_art)!!,
                isSampled = false,
                dataSource = DataSource.MEMORY
            )
        }

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
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} == ${ContentUris.parseId(data)}"
        val parent = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        // Query the data using the resolver with specified parameters.
        val data = resolver.query2(parent, projection, selection, null, limit = 1).use {
            // This section of code handles an unexpected scenario where the retrieval of album art data
            // encounters an issue. If this situation occurs, we should return the default artwork instead.
            // This is a fallback mechanism to ensure that even in the face of errors or missing data,
            // we can provide a default result.
            if (it == null || !it.moveToFirst())
                return DEFAULT_RESULT!!
            // At this point in the code, we expect that the path should not be null.
            // However, if somehow it is null, we again fall back to the default result.
            it.getString(0) ?: return DEFAULT_RESULT!!
        }

        // Initialize a MediaMetadataRetriever to work with the retrieved data.
        return MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(data)
            // fallback to default if null
            val bytes = retriever.embeddedPicture ?: return DEFAULT_RESULT!!
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

            DrawableResult(
                drawable = bitmap.toDrawable(options.context.resources),
                isSampled = isSampled,
                dataSource = DataSource.DISK
            )
        }
    }
}

