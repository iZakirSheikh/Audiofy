package com.prime.player.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils.*
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.core.graphics.drawable.toBitmap
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.prime.player.R
import com.prime.player.core.models.Audio
import kotlinx.coroutines.runBlocking
import java.util.*
import androidx.core.content.ContextCompat.startActivity

import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.core.content.ContextCompat
import kotlin.collections.ArrayList


fun getAlbumArtUri(albumId: Long): Uri {
    return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

fun getTrackUri(trackId: Long): Uri {
    return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)
}

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

fun toDuration(mills: Int): String = toDuration(mills.toLong())

fun toRelativeTimeSpan(mills: Long): CharSequence {
    return getRelativeTimeSpanString(
        mills,
        System.currentTimeMillis(),
        DAY_IN_MILLIS,
        FORMAT_ABBREV_RELATIVE
    )
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

@WorkerThread
fun Context.getAlbumArt(albumID: Long, size: Int = 512): Bitmap? {
    val uri = getAlbumArtUri(albumId = albumID)
    val r = ImageRequest.Builder(context = applicationContext)
        .data(uri)
        // We scale the image to cover 128px x 128px (i.e. min dimension == 128px)
        .size(size).scale(coil.size.Scale.FILL)
        // Disable hardware bitmaps, since Palette uses Bitmap.getPixels()
        .allowHardware(false)
        .build()
    return when (val result = runBlocking { Coil.execute(r) }) {
        is SuccessResult -> result.drawable.toBitmap()
        else -> null
    }
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
        val builder = VmPolicy.Builder()
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
        val builder = VmPolicy.Builder()
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

