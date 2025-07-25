/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 11-07-2024.
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

package com.zs.core.common

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.Uninterruptibles
import com.zs.core.db.playlists.Playlist
import com.zs.core.playback.MediaFile
import com.zs.core.store.models.Audio
import com.zs.core.store.models.Video
import com.zs.core.telemetry.Analytics
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KProperty

private const val TAG = "common-extensions"

/**
 * Executes the given [block] function within a try-catch block, logging any exceptions that occur.
 *
 * This function provides a convenient way to run code that might throw exceptions without crashing the application.
 * If an exception is caught, it will be logged using the provided [tag] and the exception's stack trace,
 * and the function will return `null`. If the block executes successfully, it returns the result of the block.
 *
 * @param tag The tag used for logging any exceptions. Typically, this should be a string representing the class or module
 *            where this function is being called.
 * @param block The function block to execute. This block can be any function that operates on the receiver object [T] and returns a value of type [R].
 * @return The result of executing the [block] function, or `null` if an exception was caught.
 *
 * Example Usage:
 * ```kotlin
 * val result = someObject.runCatching("MyClass", {
 *     // Code that might throw an exception
 *     someMethodThatMightFail()
 * })
 *
 * if (result != null) {
 *     // Process the successful result
 * } else {
 *   // Handle the exception (it was already logged)
 * }
 * ```
 */
internal inline fun <T, R> T.runCatching(tag: String, block: T.() -> R): R? {
    return try {
        block()
    } catch (e: Throwable) {
        Log.e(tag, "runCatching: $e")
        null
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
 * Checks if a given permission is granted for the application in the current context.
 *
 * @param permission The permission string tocheck.
 * @return `true` if the permission is granted, `false` otherwise.
 */
fun Context.isPermissionGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * @see isPermissionGranted
 */
fun Context.checkSelfPermissions(values: List<String>) =
    values.all { isPermissionGranted(it) }

/**
 * Shows a platform Toast message with the given text.
 *
 * This function uses the standard Android Toast class to display a short message to the user.
 *
 * @param message The text message to display in the Toast.
 * @param priority The duration of the Toast.
 */
fun Context.showPlatformToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * @see showPlatformToast
 */
fun Context.showPlatformToast(@StringRes message: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Creates a new [Intent] with the specified action and applies the given block to it.
 * This provides a concise way to create and configure an Intent.
 *
 * @param action The action string for the Intent.
 * @param block A function literal with receiver [Intent] that is applied to the new Intent instance.
 * @return The newly created and configured Intent.
 */
inline fun Intent(action: String, block: Intent.() -> Unit) =
    Intent(action).apply(block)

/**
 * Gets a value of type [T] from [SharedPreferences] with an optional default value.
 *
 * This is an inline extension function with operator overloading for the `get` operator,
 * allowing you to access values using the `[]` syntax (e.g., `prefs["key"]`).
 *
 * @param key The key for the preference.
 * @param defaultValue An optional default value to return if the key is not found.
 * @return The value of type [T] associated with the key, or `defaultValue` if the key is not found.
 *         Returns `null` if the key is not found and no default value is provided, or if the type [T] is not supported.
 */
@Suppress("UNCHECKED_CAST")
suspend inline operator fun <reified T> SharedPreferences.get(key: String, defaultValue: T): T {
    return withContext(Dispatchers.IO) {
        when (T::class) {
            String::class -> getString(key, defaultValue as? String) as T
            Int::class -> getInt(key, defaultValue as? Int ?: 0) as T
            Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T
            Float::class -> getFloat(key, defaultValue as? Float ?: 0f) as T
            Long::class -> getLong(key, defaultValue as? Long ?: 0L) as T
            Set::class -> getStringSet(key, defaultValue as? Set<String>) as T
            else -> error("Unsupported type ${T::class}")
        }
    }
}

/**
 * Puts a value of type [T] into [SharedPreferences].
 *
 * This is an inline extension function with operator overloading for the `set` operator,
 * allowing you to put values using the `[]` syntax (e.g., `prefs["key"] = value`).
 *
 * @param key The key for the preference.
 * @param value The value of type [T] to be put.
 * @return Returns `true` if the value was successfully put, `false` otherwise.
 */
@Suppress("UNCHECKED_CAST")
suspend inline operator fun <reified T> SharedPreferences.set(key: String, value: T) {
    withContext(Dispatchers.IO){
        with(edit()) {
            when (T::class) {
                String::class -> putString(key, value as? String)
                Int::class -> putInt(key, value as? Int ?: 0)
                Boolean::class -> putBoolean(key, value as? Boolean ?: false)
                Float::class -> putFloat(key, value as? Float ?: 0f)
                Long::class -> putLong(key, value as? Long ?: 0L)
                Set::class -> putStringSet(key, value as? Set<String>)
                else -> error("Unsupported type ${T::class}")
            }.apply()
        }
    }
}

/** Permits property delegation of `val`s using `by` for [Result]. */
@Suppress("NOTHING_TO_INLINE")
internal inline operator fun <T> Result<T>.getValue(thisObj: Any?, property: KProperty<*>): T? = getOrNull()

/**
 * Returns the value if this [Result] is a [Result.Success], or `null` if it is a [Result.Failure].
 */
internal operator fun <T> Result<T>.component1(): T? = getOrNull()

/**
 * Returns the exception if this [Result] is a [Result.Failure], or `null` if it is a [Result.Success].
 */
internal operator fun <T> Result<T>.component2(): Throwable? = exceptionOrNull()

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
internal suspend fun <T> ListenableFuture<T>.await(): T {
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
 * Controls whether both the system status bars and navigation bars have a light appearance.
 *
 * - When `true`, both the status bar and navigation bar will use a light theme (dark icons on a light background).
 * - When `false`, both will use a dark theme (light icons on a dark background).
 *
 * Setting this property adjusts both `isAppearanceLightStatusBars` and `isAppearanceLightNavigationBars`.
 *
 * @property value `true` to apply light appearance, `false` for dark appearance.
 */
var WindowInsetsControllerCompat.isAppearanceLightSystemBars: Boolean
    set(value) {
        isAppearanceLightStatusBars = value
        isAppearanceLightNavigationBars = value
    }
    get() = isAppearanceLightStatusBars && isAppearanceLightNavigationBars

/**
 * Gets the package info of this app using the package manager.
 * @return a PackageInfo object containing information about the app, or null if an exception occurs.
 * @see android.content.pm.PackageManager.getPackageInfo
 */
fun PackageManager.getPackageInfoCompat(pkgName: String) =
    runCatching(TAG + "_review") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
        else
            getPackageInfo(pkgName, 0)
    }

/**
 *
 */
inline fun Analytics.logEvent(name: String, block: Bundle.() -> Unit) =
    logEvent(name, Bundle().apply(block))

@OptIn(FlowPreview::class)

/**
 * Returns a flow that mirrors the original flow, but debounces emissions after the first one.
 *
 * The first emission from the original flow is emitted immediately. Subsequent emissions
 * are debounced by the specified [delayMillis].
 *
 * @param delayMillis The duration in milliseconds to debounce subsequent emissions.
 * @return A flow that debounces emissions after the first one.
 *
 * @see [debounce]
 */
fun <T> Flow<T>.debounceAfterFirst(delayMillis: Long): Flow<T> {
    var firstEmission = true
    return this
        .debounce {
            if (firstEmission) {
                firstEmission = false
                0L
            } else {
                delayMillis
            }
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
fun Audio.toTrack(playlistId: Long, order: Int) =
    Playlist.Track(
        playlistID = playlistId,
        order = order,
        uri = uri.toString(),
        title = name,
        subtitle = artist,
        artwork = artworkUri.toString(),
        mimeType = mimeType
    )

/**
 * @see com.zs.core.playback.toMediaFile(com.zs.core.store.models.Audio)
 */
fun Video.toMediaFile() =
    MediaFile(contentUri, name, "", contentUri, mimeType)

/**
 * @see Audio.toTrack
 */
fun Video.toTrack(playlistId: Long, order: Int): Playlist.Track {
    val uri = contentUri.toString()
    return Playlist.Track(playlistID = playlistId, order = order, uri = uri, title = name, subtitle = "", artwork = uri, mimeType = mimeType)
}
