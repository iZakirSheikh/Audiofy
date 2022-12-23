package com.prime.player.common

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.StrictMode
import android.text.format.DateUtils.*
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.prime.player.R
import com.prime.player.core.Audio
import com.primex.core.Text
import com.primex.core.TextPlural
import com.primex.core.runCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.annotations.Contract
import java.net.URI
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.time.ExperimentalTime


private const val TAG = "Util"

object UrlUtil

object Util

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


/**
 * Return given duration in a human-friendly format. For example, "4
 * minutes" or "1 second". Returns only largest meaningful unit of time,
 * from seconds up to hours.
 */
fun Util.formatAsDuration2(mills: Long): Text {
    return when {
        mills >= HOUR_IN_MILLIS -> {
            val hours = ((mills + 1800000) / HOUR_IN_MILLIS).toInt()
            TextPlural(R.plurals.duration_hours, hours, hours)
        }
        mills >= MINUTE_IN_MILLIS -> {
            val minutes = ((mills + 30000) / MINUTE_IN_MILLIS).toInt()
            TextPlural(R.plurals.duration_minutes, minutes, minutes)
        }
        else -> {
            val seconds = ((mills + 500) / SECOND_IN_MILLIS).toInt()
            TextPlural(R.plurals.duration_seconds, seconds, seconds)
        }
    }
}


/**
 * Return given duration in a human-friendly format. For example, "4
 * minutes" or "1 second". Returns only largest meaningful unit of time,
 * from seconds up to hours.
 *
 * @hide
 */
@Deprecated(message = "Use toDuration(mills) with Text return ", level = DeprecationLevel.ERROR)
fun Util.formatAsDuration(context: Context, mills: Long): String {
    val res = context.resources
    return when {
        mills >= HOUR_IN_MILLIS -> {
            val hours = ((mills + 1800000) / HOUR_IN_MILLIS).toInt()
            res.getQuantityString(
                R.plurals.duration_hours, hours, hours
            )
        }
        mills >= MINUTE_IN_MILLIS -> {
            val minutes = ((mills + 30000) / MINUTE_IN_MILLIS).toInt()
            res.getQuantityString(
                R.plurals.duration_minutes, minutes, minutes
            )
        }
        else -> {
            val seconds = ((mills + 500) / SECOND_IN_MILLIS).toInt()
            res.getQuantityString(
                R.plurals.duration_seconds, seconds, seconds
            )
        }
    }
}

fun Util.formatAsDuration(mills: Int): String =
    formatAsDuration(mills.toLong())


/**
 * @return mills formated as hh:mm:ss
 */
fun Util.formatAsDuration(mills: Long): String {
    var minutes: Long = mills / 1000 / 60
    val seconds: Long = mills / 1000 % 60
    return if (minutes < 60) {
        String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds)
    } else {
        val hours = minutes / 60
        minutes %= 60
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    }
}

/**
 * Returns a string describing 'time' as a time relative to 'now'.
 * <p>
 * Time spans in the past are formatted like "42 minutes ago". Time spans in
 * the future are formatted like "In 42 minutes".
 * <p>
 * Can use {@link #FORMAT_ABBREV_RELATIVE} flag to use abbreviated relative
 * times, like "42 mins ago".
 *
 * @param time the time to describe, in milliseconds
 * @param now the current time in milliseconds
 * @param minResolution the minimum timespan to report. For example, a time
 *            3 seconds in the past will be reported as "0 minutes ago" if
 *            this is set to MINUTE_IN_MILLIS. Pass one of 0,
 *            MINUTE_IN_MILLIS, HOUR_IN_MILLIS, DAY_IN_MILLIS,
 *            WEEK_IN_MILLIS
 * @param flags a bit mask of formatting options, such as
 *            {@link #FORMAT_NUMERIC_DATE} or
 *            {@link #FORMAT_ABBREV_RELATIVE}
 */
@OptIn(ExperimentalTime::class)
fun Util.formatAsRelativeTimeSpan(mills: Long) = getRelativeTimeSpanString(
    mills,
    System.currentTimeMillis(),
    DAY_IN_MILLIS,
    FORMAT_ABBREV_RELATIVE
) as String

/**
 * @return: String representation of Time like 24:34 i.e., 24min and 128 secs.
 */
@OptIn(ExperimentalTime::class)
fun Util.formatAsTime(mills: Long): String {
    var minutes: Long = mills / 1000 / 60
    val seconds: Long = mills / 1000 % 60
    return if (minutes < 60) String.format(
        Locale.getDefault(),
        "%01d:%02d",
        minutes,
        seconds
    ) else {
        val hours = minutes / 60
        minutes %= 60
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    }
}


