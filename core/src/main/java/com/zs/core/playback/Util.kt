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
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.zs.core.common.await
import com.zs.core.db.playlists.Playlist
import com.zs.core.db.playlists.Playlists
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

/**
 * Creates a [CommandButton] with the specified icon, label, and session command.
 *
 * This function simplifies the creation of a [CommandButton] by providing a
 * concise way to set its essential properties.
 *
 * @param icon The drawable resource ID for the button's icon.
 * @param label The display name (label) for the button.
 * @param command The [SessionCommand] associated with this button.
 * @return A new [CommandButton] instance.
 * @see CommandButton.Builder
 */
internal fun CommandButton(@DrawableRes icon: Int, label: String, command: SessionCommand) =
    CommandButton.Builder()
        .setIconResId(icon)
        .setDisplayName(label)
        .setSessionCommand(command)
        .build()

/**
 * Toggles the "liked" status of a [MediaItem] in the user's "Favourites" playlist.
 *
 * This function checks if the given [MediaItem] is already present in the "Favourites" playlist.
 * - If it is, the item is removed from the playlist.
 * - If it is not, the item is added to the playlist.
 *
 * The "Favourites" playlist is identified by [Remote.PLAYLIST_FAVOURITE]. If this playlist
 * does not exist, it will be created.
 *
 * @param item The [MediaItem] whose liked status is to be toggled.
 * @return `true` if the operation (add or remove) was successful, `false` otherwise.
 *         Specifically:
 *         - Returns `true` if the item was successfully removed (was liked, now unliked).
 *         - Returns `true` if the item was successfully added (was unliked, now liked).
 *         - Returns `false` if the removal failed.
 *         - Returns `false` if the insertion failed.
 */
internal suspend fun Playlists.toggleLike(item: MediaItem): Boolean {
    // Get a reference to the Playlists instance.
    val playlists = this
    // Retrieve the ID of the "Favourites" playlist.
    // If the playlist doesn't exist, create it and get its ID.
    val playlistId = playlists[Remote.PLAYLIST_FAVOURITE]?.id
        ?: playlists.insert(Playlist(Remote.PLAYLIST_FAVOURITE, ""))
    // Check if the item is already in the "Favourites" playlist.
    val isLiked = playlists.contains(Remote.PLAYLIST_FAVOURITE, item.mediaUri.toString())
    // Perform the toggle operation based on whether the item is currently liked.
    return if (isLiked)
        // removal successful? new state is "not liked"
        playlists.remove(playlistId, item.mediaUri.toString()) != 1
    else {
        // If not liked, create a new Track object for the item and attempt to insert it.
        // The play order is set to be the next available position in the playlist.
        val newTrack = item.toTrack(playlistId, playlists.lastPlayOrder(Remote.PLAYLIST_FAVOURITE) + 1)
        // The result is true if insertion was successful (the returned list of inserted tracks is not empty).
        (playlists.insert(listOf(newTrack)).isNotEmpty())
    }
}

/**
 * Creates and initializes a [MediaBrowser] instance.
 *
 * This function builds a [MediaBrowser] that connects to the `Playback` service.
 * It sets up a listener to handle connection events and other media browser callbacks.
 * The connection to the media browser service is established asynchronously.
 *
 * TODO: currently a quickfix requirement. find better alternative for component name resolution.
 *
 * @param ctx The context used to create the [MediaBrowser].
 * @param listener The [MediaBrowser.Listener] to receive callbacks from the media browser.
 * @return A [com.google.common.util.concurrent.ListenableFuture] that resolves to the connected [MediaBrowser].
 */// TODO: currently a quickfix requirement. find better alternative.
internal fun MediaBrowser(ctx: Context, listener: MediaBrowser.Listener) =
    MediaBrowser
        .Builder(ctx, SessionToken(ctx, ComponentName(ctx, Playback::class.java)))
        .setListener(listener)
        .buildAsync()