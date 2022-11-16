package com.prime.player.common

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.StrictMode
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.prime.player.core.Audio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.annotations.Contract
import java.net.URI


private const val TAG = "Util"

object UrlUtil

context (ViewModel) @Suppress("NOTHING_TO_INLINE")
inline fun <T> Flow<T>.asComposeState(initial: T): State<T> {
    val state = mutableStateOf(initial)
    onEach { state.value = it }
        .launchIn(viewModelScope)
    return state
}


object FileUtils {
    /**
     * The Unix separator character.
     */
    const val PATH_SEPARATOR = '/'

    /**
     * The extension separator character.
     * @since 1.4
     */
    const val EXTENSION_SEPARATOR = '.'

    const val HIDDEN_PATTERN = "/."
}


/**
 * Gets the name minus the path from a full fileName.
 *
 * @param path  the fileName to query
 * @return the name of the file without the path
 */
fun FileUtils.name(path: String): String = path.substring(path.lastIndexOf(PATH_SEPARATOR) + 1)

/**
 * @return parent of path.
 */
fun FileUtils.parent(path: String): String = path.replace("$PATH_SEPARATOR${name(path = path)}", "")

/**
 * Returns the file extension or null string if there is no extension. This method is a
 * convenience method for obtaining the extension of a url and has undefined
 * results for other Strings.
 * It is Assumed that Url is file
 *
 * @param url  Url of the file
 *
 * @return extension
 */
fun FileUtils.extension(url: String): String? =
    if (url.contains(EXTENSION_SEPARATOR))
        url.substring(url.lastIndexOf(EXTENSION_SEPARATOR) + 1).lowercase()
    else
        null

/**
 * Checks if the file or its ancestors are hidden in System.
 */
@Contract(pure = true)
fun FileUtils.areAncestorsHidden(path: String): Boolean = path.contains(HIDDEN_PATTERN)


/**
 * Returns [bytes] as formatted data unit.
 */
fun FileUtils.toFormattedDataUnit(
    context: Context,
    bytes: Long,
    short: Boolean = true,
) = when (short) {
    true -> android.text.format.Formatter.formatShortFileSize(context, bytes)
    else -> android.text.format.Formatter.formatFileSize(context, bytes)
}

/**
 * Gets the file name from the provided url.
 */
fun UrlUtil.name(url: String): String? {
    // val decodedUrl = Uri.decode(url) ?: return null
    //if (!url.endsWith("/") /*&& decodedUrl.indexOf('?') < 0*/) return null
    val index = url.lastIndexOf('/') + 1
    return if (index > 0) url.substring(index) else null
}

@WorkerThread
fun Context.share(audios: List<Audio>) {
    try {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putExtra(Intent.EXTRA_SUBJECT, "Sharing audio files.")
            val list = ArrayList<Uri>()
            audios.forEach {
                list.add(Uri.parse("file:///" + it.path))
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
        Toast.makeText(
            this,
            "Could not share files.,",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@WorkerThread
fun Context.share(audio: Audio) {
    try {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" + audio.path))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "audio/*"
        }
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        startActivity(Intent.createChooser(shareIntent, "Sharing " + audio.title))
    } catch (e: IllegalArgumentException) {
        // TODO the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
        e.printStackTrace()
        Toast.makeText(
            this,
            "Could not share this file,",
            Toast.LENGTH_SHORT
        ).show()
    }
}


suspend fun Context.getAlbumArt(uri: Uri, size: Int = 512): Drawable? {
    val request = ImageRequest.Builder(context = applicationContext)
        .data(uri)
        // We scale the image to cover 128px x 128px (i.e. min dimension == 128px)
        .size(size).scale(coil.size.Scale.FILL)
        // Disable hardware bitmaps, since Palette uses Bitmap.getPixels()
        .allowHardware(false)
        .build()
    return when (val result = request.context.imageLoader.execute(request)) {
        is SuccessResult -> result.drawable
        else -> null
    }
}