/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 30-09-2024.
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
import android.content.Context
import android.media.audiofx.Equalizer
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.zs.core.db.playlists.Playlists
import kotlinx.coroutines.flow.Flow


/**
 * Represents the controller for [Playback]
 */
interface Relay {

    val state: Flow<NowPlaying>
    val queue: Flow<List<MediaFile>>

    /** Ensures that controller is connected with service. */
    suspend fun connect()

    /**
     * Clears the existing [queue] and replaces it with a new queue containing the specified [values].
     * Note: The queue must only contain unique [MediaItem.mediaUri] values to ensure uniqueness.
     * so, duplicate items will be dealt with automatically.
     *
     * @param values The list of [MediaItem]s to be set in the queue.
     * @return The number of items successfully added to the queue.
     */
    suspend fun set(values: List<MediaFile>): Int

    /**
     * Adds the specified [values] to the queue. If [index] is -1, the items will be added to the
     * end of the queue; otherwise, they will be inserted at the provided index.
     * Note: The queue must only contain unique [MediaItem.mediaUri] values. If an item already
     *       exists in the queue, it will discarded.
     *
     * @param values The list of [MediaItem]s to be added to the queue.
     * @param index The optional index where the items should be inserted. If -1 (default), the
     *              items will be added to the end of the queue. Note: takes any index value
     * 				and maps it to `playlistIndex` if `shuffleModeEnabled` otherwise uses the same index.
     * @return The number of items successfully added to the queue.
     */
    suspend fun add(values: List<MediaFile>, index: Int = -1): Int

    /**
     * Removes the [MediaItem] identified by [key] from the [Playback] [queue].
     *
     * @param key The unique identifier (URI) of the [MediaItem] to be removed.
     * @return `true` if the [MediaItem] was successfully removed, `false` if the [MediaItem] was
     *         not found in the [queue].
     */
    suspend fun remove(key: Uri): Boolean

    /**
     * Clears the queue if loaded otherwise does nothing.
     */
    suspend fun clear()

    /**
     * Starts playing the underlying service.
     *
     * @param playWhenReady Pass true to start playback immediately, or false to start in a paused state.
     * @see [Player.playWhenReady]
     */
    suspend fun play(playWhenReady: Boolean = true)

    /**
     * Toggles the shuffle mode for media playback and returns the new state of the shuffle mode.
     *
     * This function allows toggling the shuffle mode for media playback. If shuffle mode is currently enabled, calling
     * this function will disable it, and if it's currently disabled, calling this function will enable it.
     *
     * @return The new state of the shuffle mode after toggling. `true` if shuffle mode is enabled, `false` if it's disabled.
     */
    suspend fun toggleShuffle(): Boolean

    /** Pauses the underlying media service if it's currently playing; otherwise, does nothing. */
    suspend fun pause()

    /**
     * Skips to the next track in the playlist.
     *
     * This function allows skipping to the next track in the media playback queue. If there is no next track,
     * this function has no effect.
     *
     */
    suspend fun skipToNext()

    /**
     * Skips to the previous track in the playlist.
     *
     * This function allows skipping to the previous track in the media playback queue. If there is no previous track,
     * this function has no effect.
     */
    suspend fun skipToPrev()

    /**
     * @see Player.seekTo
     */
    @OptIn(UnstableApi::class)
    suspend fun seekTo(position: Int = POSITION_UNSET, mills: Long = TIME_UNSET)

    /**
     * Cycles through different repeat modes for media playback.
     *
     * This function changes the repeat mode for media playback. Each time this function is called,
     * the media player will switch to the next available repeat mode in a circular manner.
     *
     * @return The new repeat mode after cycling.
     */
    suspend fun cycleRepeatMode(): Int

    /**
     * Seeks to the specified position in the track associated with the given [uri].
     *
     * This function allows seeking to a specific position in the track associated with the provided [uri].
     * If the [uri] is not found or unavailable, this function will have no effect.
     *
     * @param uri The URI of the track for which the seek operation should be performed.
     */
    suspend fun seekTo(uri: Uri, mills: Long = TIME_UNSET): Boolean

    /**
     * Moves a media item from the source position [from] to the destination position [to] in the
     * playback queue.
     *
     * This function invokes the underlying media player's [Player.moveMediaItems] method to move a
     * media item from
     * the source position [from] to the destination position [to] in the playback queue. It returns
     * `true` if the move operation is successful, and `false` otherwise.
     *
     * @param from The source position of the media item to be moved.
     * @param to The destination position where the media item will be moved.
     * @return `true` if the move operation is successful, `false` otherwise.
     * @see Player.moveMediaItems
     */
    suspend fun move(from: Int, to: Int): Boolean

    /**
     *  A function that sets the sleep timer for the media player
     *
     *  The sleep timer is a feature that allows the user to set a time in the future when the
     *  playback should be paused or slept.
     *  @param mills The time in milliseconds when playback is scheduled to be paused. If the value is
     *  [Playback.UNINITIALIZED_SLEEP_TIME_MILLIS], the sleep timer is disabled or removed.
     */
    suspend fun setSleepTimeAt(mills: Long)

    /**
     * Sets a new equalizer in the [Playback]. If the [eq] parameter is null, the equalizer settings
     * will be overridden and turned off. Note that after calling this function, the previous equalizer
     * will be automatically released.
     *
     * @param eq The new Equalizer instance to be set, or null to turn off the equalizer.
     */
    suspend fun setEqualizer(eq: Equalizer?)

    /**
     * Constructs a new equalizer based on the settings received through the playback and returns the
     * new Equalizer instance.
     *
     * The equalizer in the playback has its property set to 0. To activate the equalizer, use a value
     * higher than 0 for the [priority] parameter. If the equalizer in playback is not set up, an
     * Equalizer with default configuration will be returned.
     *
     * @param priority The priority level for the new equalizer. Use a value higher than 0 to activate it.
     * @return The newly created Equalizer instance.
     */
    suspend fun getEqualizer(priority: Int): Equalizer


    @SuppressLint("UnsafeOptInUsageError")
    companion object {

        const val UNINITIALIZED_SLEEP_TIME_MILLIS = -1L

        const val POSITION_UNSET = C.POSITION_UNSET
        const val TIME_UNSET = C.TIME_UNSET

        // The standard global playlists.
        val PLAYLIST_FAVOURITE = Playlists.PRIVATE_PLAYLIST_PREFIX + "favourite"
        val PLAYLIST_RECENT = Playlists.PRIVATE_PLAYLIST_PREFIX + "recent"
        internal val PLAYLIST_QUEUE = Playlists.PRIVATE_PLAYLIST_PREFIX + "queue"

        const val REPEAT_MODE_ONE = Player.REPEAT_MODE_ONE
        const val REPEAT_MODE_ALL = Player.REPEAT_MODE_ALL
        const val REPEAT_MODE_OFF = Player.REPEAT_MODE_OFF

        /**
         * Constructs a new instance of [Relay].
         */
        operator fun invoke(context: Context): Relay = RelayImpl()
    }
}

