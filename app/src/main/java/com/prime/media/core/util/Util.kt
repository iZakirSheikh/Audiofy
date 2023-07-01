package com.prime.media.core.util

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.StrictMode
import android.widget.Toast
import androidx.annotation.WorkerThread
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.Uninterruptibles
import com.prime.media.core.db.Audio
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "Util"

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
    get() = com.primex.core.runCatching(TAG) {
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