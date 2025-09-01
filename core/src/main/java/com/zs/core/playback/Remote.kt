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
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.annotation.FloatRange
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.SessionCommand
import com.zs.core.db.playlists.Playlists
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


/**
 * Represents the controller for [Playback]
 */
interface Remote {

    @SuppressLint("UnsafeOptInUsageError")
    companion object {

        const val POSITION_UNSET = C.POSITION_UNSET
        const val TIME_UNSET = C.TIME_UNSET
        const val INDEX_UNSET = C.INDEX_UNSET

        // Represents all the update events that trigger state change.
        internal val STATE_UPDATE_EVENTS = intArrayOf(
            Player.EVENT_TIMELINE_CHANGED,
            Player.EVENT_PLAYBACK_STATE_CHANGED,
            Player.EVENT_REPEAT_MODE_CHANGED,
            Player.EVENT_IS_PLAYING_CHANGED,
            Player.EVENT_IS_LOADING_CHANGED,
            Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
            Player.EVENT_MEDIA_ITEM_TRANSITION,
            Player.EVENT_VIDEO_SIZE_CHANGED
        )

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

        // Commands
        private const val PREFIX = "com.prime.player"
        internal val AUDIO_SESSION_ID = SessionCommand("_audio_session_id", Bundle.EMPTY)
        internal const val EXTRA_AUDIO_SESSION_ID = "_audio_session_id"
        internal val SCHEDULE_SLEEP_TIME = SessionCommand("_schedule_sleep_time", Bundle.EMPTY)
        internal const val EXTRA_SCHEDULED_TIME_MILLS = "_audio_session_id"
        internal val EQUALIZER_CONFIG = SessionCommand("_equalizer_config", Bundle.EMPTY)
        internal const val EXTRA_EQUALIZER_PROPERTIES = "_extra_equalizer_properties"
        internal const val EXTRA_EQUALIZER_ENABLED = "_extra_equalizer_enabled"
        internal val SCRUBBING_MODE = SessionCommand("_scrubbing_mode", Bundle.EMPTY)
        internal const val EXTRA_SCRUBBING_MODE_ENABLED = "_extra_scrubbing_mode_enabled"
        internal val TOGGLE_LIKE = SessionCommand("_toggle_like", Bundle.EMPTY)

        internal val commands get() = arrayOf(
            AUDIO_SESSION_ID,
            SCHEDULE_SLEEP_TIME,
            EQUALIZER_CONFIG,
            SCRUBBING_MODE,
            TOGGLE_LIKE
        )

        // The roots for accessing global playlists
        internal const val ROOT_QUEUE = "com.prime.player.queue"


        /**
         * Constructs a new instance of [Remote].
         */
        operator fun invoke(context: Context): Remote = RemoteImpl(context)
    }

    val state: StateFlow<NowPlaying?>
    val queue: Flow<List<MediaFile>?>
    suspend fun getViewProvider(): VideoProvider

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

    /**
     * Toggles the Like status of the current media item at index.
     */
    suspend fun toggleLike(index: Int = -1)

    /**
     * Removes the track identified by the given [uri] from the playback queue.
     *
     * @param uri The [Uri] of the media file to be removed.
     * @return `true` if the track was successfully removed from the queue, `false` otherwise (e.g., if the track was not found in the queue).
     * @see [Player.removeMediaItem]
     */
    suspend fun remove(uri: Uri): Boolean

    /**
     * Skips to the media item identified by the given [uri] within the current queue.
     *
     * @param uri The [Uri] of the media file to be played.
     * @return `true` if the media item was found in the queue and playback was successfully initiated,
     *         `false` otherwise (e.g., if the media item was not found or an error occurred during playback initiation).
     * @see Player.seekToNextMediaItem
     */
    suspend fun skipTo(uri: Uri): Boolean

    /**
     * Sets the playback speed of the media.
     */
    suspend fun setPlaybackSpeed(@FloatRange(from = 0.0, fromInclusive = false) value: Float): Boolean
    @CheckResult suspend fun getPlaybackSpeed(): Float

    /**
     *  A function that sets the sleep timer for the media player
     *
     *  The sleep timer is a feature that allows the user to set a time in the future when the
     *  playback should be paused or slept.
     *  @param mills The time in milliseconds when playback is scheduled to be paused. If the value is
     *  [Playback.TIME_UNSET], the sleep timer is disabled or removed.
     */
    suspend fun setSleepTimeAt(mills: Long)

    /**
     *   A function that returns the sleep timer value of the media player.
     *
     *   The sleep timer is a feature that allows the user to set a time in the future when the
     *   playback should be paused or slept.
     *   @return The time in milliseconds when playback is scheduled to be paused or
     *   [Remote.TIME_UNSET] to cancel the already one.
     * */
    suspend fun getSleepTimeAt(): Long

    suspend fun isPlaying(): Boolean
}