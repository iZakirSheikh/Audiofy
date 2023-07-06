package com.prime.media.core.util

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.StrictMode
import android.provider.MediaStore
import android.text.format.DateUtils.*
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.Uninterruptibles
import com.prime.media.Audiofy
import com.prime.media.core.db.Album
import com.prime.media.core.db.Audio
import com.prime.media.core.db.Playlist
import com.prime.media.impl.Repository
import com.primex.core.runCatching
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "Util"

context (ViewModel) @Suppress("NOTHING_TO_INLINE")
@Deprecated("find new solution.")
inline fun <T> Flow<T>.asComposeState(initial: T): State<T> {
    val state = mutableStateOf(initial)
    onEach { state.value = it }.launchIn(viewModelScope)
    return state
}


@Deprecated("use imageLoader on context.")
suspend fun Context.getAlbumArt(uri: Uri, size: Int = 512): Drawable? {
    val request = ImageRequest.Builder(context = applicationContext).data(uri)
        // We scale the image to cover 128px x 128px (i.e. min dimension == 128px)
        .size(size).scale(coil.size.Scale.FILL)
        // Disable hardware bitmaps, since Palette uses Bitmap.getPixels()
        .allowHardware(false).build()
    return when (val result = request.context.imageLoader.execute(request)) {
        is SuccessResult -> result.drawable
        else -> null
    }
}

//language=RegExp
private val ISO6709LocationPattern = Pattern.compile("([+\\-][0-9.]+)([+\\-][0-9.]+)")

/**
 * This method parses the given string representing a geographic point location by coordinates in ISO 6709 format
 * and returns the latitude and the longitude in float. If `location` is not in ISO 6709 format,
 * this method returns `null`
 *
 * @param location a String representing a geographic point location by coordinates in ISO 6709 format
 * @return `null` if the given string is not as expected, an array of floats with size 2,
 * where the first element represents latitude and the second represents longitude, otherwise.
 */
val MediaMetadataRetriever.latLong: DoubleArray?
    get() = runCatching(TAG) {
        val location =
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION) ?: return@runCatching null
        val m: Matcher = ISO6709LocationPattern.matcher(location)
        if (m.find() && m.groupCount() == 2) {
            val latstr: String = m.group(1) ?: return@runCatching null
            val lonstr: String = m.group(2) ?: return@runCatching null
            val lat = latstr.toDouble()
            val lon = lonstr.toDouble()
            doubleArrayOf(lat, lon)
        } else null
    }


@WorkerThread
@Deprecated("find better solution.")
fun Context.share(audios: List<Audio>) {
    try {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putExtra(Intent.EXTRA_SUBJECT, "Sharing audio files.")
            val list = ArrayList<Uri>()
            audios.forEach {
                list.add(Uri.parse("file:///" + it.data))
            }
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, list)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "audio/*"
            //addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        startActivity(Intent.createChooser(shareIntent, "Sharing audio files..."))
    } catch (e: IllegalArgumentException) {
        // TODO the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
        e.printStackTrace()
        Toast.makeText(this, "Could not share files.,", Toast.LENGTH_SHORT).show()
    }
}

@WorkerThread
@Deprecated("find better solution")
fun Context.share(audio: Audio) {
    try {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" + audio.data))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "audio/*"
        }
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        startActivity(Intent.createChooser(shareIntent, "Sharing " + audio.name))
    } catch (e: IllegalArgumentException) {
        // TODO the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
        e.printStackTrace()
        Toast.makeText(this, "Could not share this file,", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Awaits completion of `this` [ListenableFuture] without blocking a thread.
 *
 * This suspend function is cancellable.
 *
 * If the [Job] of the current coroutine is cancelled or completed while this suspending function is waiting, this function
 * stops waiting for the future and immediately resumes with [CancellationException][kotlinx.coroutines.CancellationException].
 *
 * This method is intended to be used with one-shot Futures, so on coroutine cancellation, the Future is cancelled as well.
 * If cancelling the given future is undesired, use [Futures.nonCancellationPropagating] or
 * [kotlinx.coroutines.NonCancellable].
 */
public suspend fun <T> ListenableFuture<T>.await(): T {
    try {
        if (isDone) return Uninterruptibles.getUninterruptibly(this)
    } catch (e: ExecutionException) {
        // ExecutionException is the only kind of exception that can be thrown from a gotten
        // Future, other than CancellationException. Cancellation is propagated upward so that
        // the coroutine running this suspend function may process it.
        // Any other Exception showing up here indicates a very fundamental bug in a
        // Future implementation.
        throw e.cause!!
    }

    return suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
        addListener(
            ToContinuation(this, cont),
            MoreExecutors.directExecutor()
        )
        cont.invokeOnCancellation {
            cancel(false)
        }
    }
}

/**
 * Propagates the outcome of [futureToObserve] to [continuation] on completion.
 *
 * Cancellation is propagated as cancelling the continuation. If [futureToObserve] completes
 * and fails, the cause of the Future will be propagated without a wrapping
 * [ExecutionException] when thrown.
 */
private class ToContinuation<T>(
    val futureToObserve: ListenableFuture<T>,
    val continuation: CancellableContinuation<T>
) : Runnable {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun run() {
        if (futureToObserve.isCancelled) {
            continuation.cancel()
        } else {
            try {
                continuation.resume(Uninterruptibles.getUninterruptibly(futureToObserve))
            } catch (e: ExecutionException) {
                // ExecutionException is the only kind of exception that can be thrown from a gotten
                // Future. Anything else showing up here indicates a very fundamental bug in a
                // Future implementation.
                continuation.resumeWithException(e.cause!!)
            }
        }
    }
}


/**
 * Adds the specified element to the list if it is not already present.
 *
 * @param value the value to add to the list
 * @return `true` if the element was added, `false` if the list already contains the element
 */
inline fun <T> MutableList<T>.addDistinct(value: T): Boolean {
    return if (contains(value)) return false
    else add(value)
}


fun Context.launchPlayStore() {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Audiofy.GOOGLE_STORE)).apply {
            setPackage(Audiofy.PKG_GOOGLE_PLAY_STORE)
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Audiofy.FALLBACK_GOOGLE_STORE)))
    }
}

/**
 * Factory function that creates a `Member` object with the given `MediaItem` object and some additional metadata.
 *
 * @param from The `MediaItem` object to create the `Member` from.
 * @param playlistId The ID of the playlist to which this `Member` belongs.
 * @param order The order of this `Member` within the playlist.
 * @return A new `Member` object with the given parameters.
 */
fun Member(from: MediaItem, playlistId: Long, order: Int) =
    Playlist.Member(
        playlistId,
        from.mediaId,
        order,
        from.requestMetadata.mediaUri!!.toString(),
        from.mediaMetadata.title.toString(),
        from.mediaMetadata.subtitle.toString(),
        from.mediaMetadata.artworkUri?.toString()
    )

/**
 * @see Member
 */
fun Member(from: Audio, playlistId: Long, order: Int) =
    Playlist.Member(
        playlistId,
        "${from.id}",
        order,
        from.uri.toString(),
        from.name,
        from.artist,
        from.albumUri.toString()
    )

/**
 * @see MediaItem.key
 */
val Playlist.Member.key get() = uri

/**
 * This is a read-only property that returns the key of the `MediaItem`. The key is generated by converting the media URI
 * of the `MediaItem` to a string.
 * This key is unique wrt list etc.
 *
 * @return The key of the `MediaItem`.
 *
 * @throws NullPointerException If the media URI of the `MediaItem` is null.
 */
val MediaItem.key get() = requestMetadata.mediaUri!!.toString()

/**
 * Creates a new [MediaItem] instance using the provided parameters.
 *
 * @param uri The URI of the media item.
 * @param title The title of the media item.
 * @param subtitle The subtitle of the media item.
 * @param id The unique identifier of the media item. Defaults to [MediaItem.DEFAULT_MEDIA_ID].
 * @param artwork The URI of the artwork for the media item. Defaults to null.
 * @return The new [MediaItem] instance.
 */
fun MediaItem(
    uri: Uri,
    title: String,
    subtitle: String,
    id: String = "non_empty",
    artwork: Uri? = null,
) =
    MediaItem.Builder()
        .setMediaId(id)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(
            MediaMetadata.Builder().setArtworkUri(artwork).setTitle(title).setSubtitle(subtitle)
                .setFolderType(MediaMetadata.FOLDER_TYPE_NONE).setIsPlayable(true)
                // .setExtras(bundleOf(ARTIST_ID to artistId, ALBUM_ID to albumId))
                .build()
        ).build()

/**
 * Returns the content URI for this audio file, using the [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI]
 * and appending the file's unique ID.
 *
 * @return the content URI for the audio file
 */
val Audio.uri
    get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

/**
 * Returns the content URI for the album art image of this audio file's album, using the
 * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] and appending the album ID to the end of the URI.
 *
 * @return the content URI for the album art image of this audio file's album
 */
val Audio.albumUri
    get() = Repository.toAlbumArtUri(albumId)

/**
 * Returns the content URI for the album art image of this album, using the [MediaStore.Images.Media.EXTERNAL_CONTENT_URI]
 * and appending the album's unique ID to the end of the URI.
 *
 * @return the content URI for the album art image of this album
 */
val Album.uri
    get() = Repository.toAlbumArtUri(id)

@Deprecated("Use the factory one.")
inline fun Audio.toMember(playlistId: Long, order: Int) = Member(this, playlistId, order)



/**
 * Returns the content URI for this audio file as a string, using the [uri] property of the audio file.
 *
 * @return the content URI for this audio file as a string
 */
val Audio.key get() = uri.toString()

/**
 * Returns a [MediaItem] object that represents this audio file as a playable media item.
 *
 * @return the [MediaItem] object that represents this audio file
 */
inline val Audio.toMediaItem
    get() = MediaItem(uri, name, artist, "$id", albumUri)

/**
 * Returns a [MediaItem] object that represents this [Member] file as a playable media item.
 *
 * @return the [MediaItem] object that represents this audio file
 */
inline val Playlist.Member.toMediaItem
    get() = MediaItem(Uri.parse(uri), title, subtitle, id, artwork?.let { Uri.parse(it) })