package com.prime.player.common

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils.*
import android.util.Log
import android.util.TypedValue
import androidx.annotation.WorkerThread
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.prime.player.R
import com.primex.core.runCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.Contract
import java.io.Closeable
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.time.ExperimentalTime


private const val TAG = "Util"


object Utils {
    /**
     * @return: String representation of Time like 24:34 i.e., 24min and 128 secs.
     */
    @OptIn(ExperimentalTime::class)
    fun formatAsTime(mills: Long): String {
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
    fun formatAsRelativeTimeSpan(mills: Long) = getRelativeTimeSpanString(
        mills,
        System.currentTimeMillis(),
        DAY_IN_MILLIS,
        FORMAT_ABBREV_RELATIVE
    ) as String

    fun toDuration(mills: Long): String {
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
     * Return given duration in a human-friendly format. For example, "4
     * minutes" or "1 second". Returns only largest meaningful unit of time,
     * from seconds up to hours.
     *
     * @hide
     */
    fun toDuration(context: Context, mills: Long): String {
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
}


/**
 * This Method retreves Color From Attributes Both Default and Custom
 *
 * @param attr
 * the attribute from which color is to be retrieved
 * @param context
 * The context of Application
 *
 * @return Hex String of corresponding Color
 */
fun Context.getColorFromAttr(attr: Int): Int {
    return try {
        theme.resources.getColor(attr)
    } catch (exception: Exception) {
        val typedValue = TypedValue()
        val theme: Resources.Theme = theme
        theme.resolveAttribute(attr, typedValue, true)
        typedValue.data
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


context (ViewModel) @Suppress("NOTHING_TO_INLINE")
inline fun <T> Flow<T>.toComposeState(initial: T): State<T> {
    val state = mutableStateOf(initial)
    onEach { state.value = it }
        .launchIn(viewModelScope)
    return state
}