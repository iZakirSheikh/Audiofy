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
import android.net.Uri
import androidx.annotation.FloatRange
import androidx.media3.common.C
import androidx.media3.common.Player
import com.zs.core.db.playlists.Playlists
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


/**
 * Represents the controller for [Playback]
 */
interface Remote {

    @SuppressLint("UnsafeOptInUsageError")
    companion object {

        const val SLEEP_TIME_UNSET = -1L

        const val POSITION_UNSET = C.POSITION_UNSET
        const val TIME_UNSET = C.TIME_UNSET
        const val INDEX_UNSET = C.INDEX_UNSET

        // The standard global playlists.
        val PLAYLIST_FAVOURITE = Playlists.PRIVATE_PLAYLIST_PREFIX + "favourite"
        val PLAYLIST_RECENT = Playlists.PRIVATE_PLAYLIST_PREFIX + "recent"
        internal val PLAYLIST_QUEUE = Playlists.PRIVATE_PLAYLIST_PREFIX + "queue"

        const val REPEAT_MODE_ONE = Player.REPEAT_MODE_ONE
        const val REPEAT_MODE_ALL = Player.REPEAT_MODE_ALL
        const val REPEAT_MODE_OFF = Player.REPEAT_MODE_OFF

        // State
        const val PLAYER_STATE_IDLE = Player.STATE_IDLE
        const val PLAYER_STATE_BUFFERING = Player.STATE_BUFFERING
        const val PLAYER_STATE_READY = Player.STATE_READY
        const val PLAYER_STATE_ENDED = Player.STATE_ENDED

        //
        private const val PREFIX = "com.prime.player"
        internal const val ACTION_AUDIO_SESSION_ID = "$PREFIX.action.AUDIO_SESSION_ID"
        internal const val EXTRA_AUDIO_SESSION_ID = "$PREFIX.extra.AUDIO_SESSION_ID"
        internal const val ACTION_SCHEDULE_SLEEP_TIME = "$PREFIX.action.SCHEDULE_SLEEP_TIME"
        internal const val EXTRA_SCHEDULED_TIME_MILLS = "$PREFIX.extra.AUDIO_SESSION_ID"
        internal const val ACTION_EQUALIZER_CONFIG = "$PREFIX.extra.EQUALIZER"
        internal const val EXTRA_EQUALIZER_ENABLED = "$PREFIX.extra.EXTRA_EQUALIZER_ENABLED"
        internal const val EXTRA_EQUALIZER_PROPERTIES = "$PREFIX.extra.EXTRA_EQUALIZER_PROPERTIES"

        // The roots for accessing global playlists
        internal const val ROOT_QUEUE = "com.prime.player.queue"


        /**
         * Constructs a new instance of [Remote].
         */
        operator fun invoke(context: Context): Remote = RemoteImpl(context)
    }

    val state: StateFlow<NowPlaying?>
    val queue: Flow<List<MediaFile>>

    suspend fun setMediaFiles(values: List<MediaFile>): Int

    /**
     * Starts playing the underlying service.
     *
     * @param playWhenReady Pass true to start playback immediately, or false to start in a paused state.
     * @see [Player.playWhenReady]
     */
    suspend fun play(playWhenReady: Boolean = true)
    suspend fun pause()

    /**
     * Sets the repeat [mode] of the player.
     * @see Player.setRepeatMode
     */
    suspend fun setRepeatMode(mode: Int)

    suspend fun togglePlay()

    suspend fun shuffle(shuffle: Boolean)

    suspend fun skipToNext()
    suspend fun skipToPrevious()

    /** Skips to [index] in queue and seeks to [mills]; returns `true` if successful. */
    suspend fun seekTo(index: Int = INDEX_UNSET, mills: Long = TIME_UNSET): Boolean

    suspend fun seekTo(@FloatRange(0.0, 1.0) pct: Float)

    /** Returns the index of [MediaFile] represented by the [uri] or [Remote.INDEX_UNSET] */
    suspend fun indexOf(uri: Uri): Int

    /** Clears the queue if loaded otherwise does nothing. */
    suspend fun clear()

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
     * Returns the index of the next media item in the current playlist, or [Remote.INDEX_UNSET] if there is no next item.
     */
    suspend fun getNextMediaItemIndex(): Int

    /**
     * Returns the index of the current media item in the playlist, or [Remote.INDEX_UNSET] if the playlist is empty or no item is currently playing.
     */
    suspend fun getCurrentMediaItemIndex(): Int

    /**
     * Cycles through the available repeat modes: [Remote.REPEAT_MODE_OFF], [Remote.REPEAT_MODE_ONE],
     * and [Remote.REPEAT_MODE_ALL].
     *
     * @return The new repeat mode after cycling.
     */
    suspend fun cycleRepeatMode(): Int
}