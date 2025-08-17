/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 16-06-2025.
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

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.zs.core.common.await
import com.zs.core.db.playlists.Playlist
import com.zs.core.store.models.Audio

/**
 * Creates a new instance of [NextRenderersFactory] using reflection. The renderer is provided as a dynamic feature module and might not be available at install time. The feature needs to be added to the APK on-demand.
 *
 * @param context The application context.
 * @return A new instance of [DefaultRenderersFactory], or `null` if an error occurs.
 */
@SuppressLint("UnsafeOptInUsageError")
internal fun DynamicRendererFactory(context: Context): DefaultRenderersFactory? {
    return runCatching() {
        val codexClass =
            Class.forName("com.zs.feature.codex.CodexKt") // Assuming the functionis in a Kotlin file named Codex.kt
        val codexMethod = codexClass.getDeclaredMethod("Codex", Context::class.java)
        codexMethod.invoke(
            null,
            context
        ) as? DefaultRenderersFactory // Static method, so first argument is null
    }.getOrNull()
}


/**
 * Checks if the given URI is from a third-party source.
 *
 * This property evaluates whether the URI scheme is "content://" and the authority
 * is not equal to [MediaStore.AUTHORITY]. If these conditions are met, it indicates
 * that the URI is from a third-party source.
 *
 * @context uri The URI to be checked.
 * @return `true` if the URI is from a third-party source, `false` otherwise.
 */
internal val Uri.isThirdPartyUri
    get() = scheme == ContentResolver.SCHEME_CONTENT && authority != MediaStore.AUTHORITY

/**
 * Returns all the [MediaItem]s of [Player] in their natural order.
 *
 * @return A list of [MediaItem]s in the player's natural order.
 */
inline val Player.mediaItems get() = List(this.mediaItemCount, ::getMediaItemAt)

/**
 * returns the positions array from the [DefaultShuffleOrder]
 *
 * FixMe: Extracts array from player using reflection.
 */
internal val Player.orders: IntArray
    @OptIn(UnstableApi::class)
    get() {
        require(this is ExoPlayer)
        val f1 = this.javaClass.getDeclaredField("shuffleOrder")
        f1.isAccessible = true
        val order2 = f1.get(this)
        require(order2 is DefaultShuffleOrder)
        val f2 = order2.javaClass.getDeclaredField("shuffled")
        f2.isAccessible = true
        return f2.get(order2) as IntArray
    }

/**
 * The queue property represents the list of media items in the player's queue.
 * If shuffle mode is not enabled, the queue will contain the media items in their natural order.
 * If shuffle mode is enabled, the queue will contain the media items in the order specified by the 'orders' list.
 *
 * @return The list of media items in the player's queue.
 */
val Player.queue get() = if (!shuffleModeEnabled) mediaItems else orders.map(::getMediaItemAt)

/**
 * A short-hand for creating a [SessionResult] with the given code and arguments.
 *
 * @param code The result code.
 * @param args A lambda function to apply additional arguments to the [Bundle].
 * @see [SessionResult]
 */
internal inline fun SessionResult(code: Int, args: Bundle.() -> Unit) =
    SessionResult(code, Bundle().apply(args))

/**
 * Converts a [Playlist.Track] to a [MediaFile].
 *
 * This function takes a [Playlist.Track] object and transforms it into a [MediaFile] object.
 * The [MediaFile] will have its URI, title, subtitle, artwork URI, and MIME type
 * populated from the corresponding fields of the [Playlist.Track].
 *
 * @return A [MediaFile] representation of the [Playlist.Track].
 */
fun Playlist.Track.toMediaFile() = MediaFile(
    Uri.parse(uri),
    title = title,
    subtitle = subtitle,
    artwork?.toUri(),
    mimeType
)

/**
 * Converts an [Audio] object to a [MediaFile].
 *
 * This function takes an [Audio] object and transforms it into a [MediaFile] object.
 * The [MediaFile] will have its URI, name, artist, artwork URI, and MIME type
 * populated from the corresponding fields of the [Audio] object.
 *
 * @return A [MediaFile] representation of the [Audio] object.
 */
fun Audio.toMediaFile() =
    MediaFile(uri, name, artist, artworkUri, mimeType)

/**
 * Lightweight wrapper around a [Player] to manage video surfaces.
 *
 * Provides safe checks ([isEmpty], [canSetVideoSurface]) and helper
 * methods to attach or clear [SurfaceView] / [TextureView] as video surfaces.
 */
@JvmInline
value class VideoProvider(val value: Any? = null) {

    /** `true` if no player is attached. */
    val isEmpty: Boolean
        get() = value == null

    /** `true` if the player supports [Player.COMMAND_SET_VIDEO_SURFACE]. */
    val canSetVideoSurface: Boolean
        get() = (value as? Player)?.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE) == true

    /**
     * Attaches the given [view] as a video surface.
     *
     * @throws IllegalArgumentException if the view is not [SurfaceView] or [TextureView].
     */
    fun setVideoSurfaceView(view: View) {
        when (view) {
            is SurfaceView -> (value as? Player)?.setVideoSurfaceView(view)
            is TextureView -> (value as? Player)?.setVideoTextureView(view)
            else -> throw IllegalArgumentException("Invalid view type $view")
        }
    }

    /**
     * Clears the given [view] from being used as a video surface.
     *
     * @throws IllegalArgumentException if the view is not [SurfaceView] or [TextureView].
     */
    fun clearVideoSurfaceView(view: View) {
        when (view) {
            is SurfaceView -> (value as? Player)?.clearVideoSurfaceView(view)
            is TextureView -> (value as? Player)?.clearVideoTextureView(view)
            else -> throw IllegalArgumentException("Invalid view type $view")
        }
    }
}

/**
 * Wrapper around [VideoSize] providing convenience accessors.
 *
 * Helps check if the size is specified and exposes width, height,
 * and pixel aspect ratio directly.
 */
@JvmInline
value class VideoSize(private val value: VideoSize = VideoSize.UNKNOWN) {

    /** `true` if the size is not [VideoSize.UNKNOWN]. */
    val isSpecified: Boolean
        get() = value != VideoSize.UNKNOWN

    /** Video width in pixels. */
    val width: Int
        get() = value.width

    /** Video height in pixels. */
    val height: Int
        get() = value.height

    /** Pixel width-to-height ratio. */
    val ratio: Float
        get() = value.pixelWidthHeightRatio
}

/**
 * Sends a custom command to the [MediaBrowser] with the given arguments.
 *
 * @param command The command string.
 * @param args A lambda function to apply arguments to the [Bundle].
 * @return The [SessionResult] of the command.
 */
internal suspend inline operator fun MediaBrowser.set(command: SessionCommand, args: Bundle) {
    sendCustomCommand(command, args).await()
}

/**
 * Sends a custom command to the MediaBrowser and awaits the result.
 *
 * This operator function provides a concise way to send a [SessionCommand]
 * to a [MediaBrowser] and suspend execution until the [SessionResult] is available.
 * It uses an empty [Bundle] for the command arguments.
 *
 * @param command The [SessionCommand] to send.
 * @return The [SessionResult] from the MediaBrowser.
 * @see MediaBrowser.sendCustomCommand
 * @see com.zs.core.common.await
 */
internal suspend inline operator fun MediaBrowser.get(command: SessionCommand) =
    sendCustomCommand(command, Bundle.EMPTY).await()




