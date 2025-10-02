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
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.annotation.FloatRange
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.session.SessionCommand
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.Remote.Companion.TRACK_TYPE_AUDIO
import com.zs.core.playback.Remote.Companion.TRACK_TYPE_TEXT
import com.zs.core.playback.Remote.Companion.TRACK_TYPE_VIDEO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


/**
 * Represents the controller for [Playback]
 */
interface Remote {

    data class TrackInfo(val name: String, internal val params: TrackSelectionOverride)

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

        // track types
        const val TRACK_TYPE_TEXT = C.TRACK_TYPE_TEXT
        const val TRACK_TYPE_AUDIO = C.TRACK_TYPE_AUDIO
        const val TRACK_TYPE_VIDEO = C.TRACK_TYPE_VIDEO

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

    suspend fun seekBy(increment: Long): Boolean

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
     * Sets a sleep timer for playback.
     *
     * @param millis Duration in **milliseconds** after which playback should pause.
     *               For example: 10 minutes → `10 * 60 * 1000`.
     *               Use [Remote.TIME_UNSET] to cancel any active timer.
     */
    suspend fun setSleepTimer(mills: Long)

    /**
     * Gets the remaining duration of the active sleep timer.
     *
     * @return Remaining time in **milliseconds**, or [Remote.TIME_UNSET] if no timer is active.
     *         For example: a return value of `300000` = 5 minutes left.
     */
    suspend fun getRemainingSleepTime(): Long

    suspend fun isPlaying(): Boolean

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


    /**
     * Retrieves the current [NowPlaying] information.
     *
     * @return The [NowPlaying] object representing the currently playing media, or `null` if no media is playing.
     */
    suspend fun getNowPlaying(): NowPlaying?

    /**
     * Sets the media item to be played using the provided [Uri].
     * This function prepares the player with the specified media item.
     * Playback does not start automatically; use [play] to start playback.
     *
     * @param uri The [Uri] of the media item to be set.
     */
    suspend fun setMediaItem(uri: Uri)


    /**
     * Retrieves a list of available tracks of a specific [type].
     *
     * This function queries the underlying player for all tracks that match the given [type]
     * (e.g., [TRACK_TYPE_AUDIO], [TRACK_TYPE_VIDEO], [TRACK_TYPE_TEXT]).
     *
     * @param type The type of tracks to retrieve. See [C.TrackType] for available types.
     * @return A list of [TrackInfo] objects, each representing an available track of the specified type.
     *         Returns an empty list if no tracks of the given type are available or if the player is not prepared.
     */
    suspend fun getAvailableTracks(type: Int): List<TrackInfo>

    /**
     * Retrieves the currently selected track for a given [type].
     *
     * @param type The type of track to query (e.g., [Remote.TRACK_TYPE_AUDIO], [Remote.TRACK_TYPE_VIDEO], [Remote.TRACK_TYPE_TEXT]).
     * @return A [TrackInfo] object representing the selected track, or `null` if no track of the specified type is selected or available.
     */
    suspend fun getSelectedTrackFor(type: Int): TrackInfo?

    /**
     * Sets the track to be played for the specified [type].
     *
     * @param info The [TrackInfo] representing the track to be selected.
     * @param type The type of track to be set (e.g., [TRACK_TYPE_AUDIO], [TRACK_TYPE_VIDEO], [TRACK_TYPE_TEXT]).
     * @return `true` if the track was successfully set, `false` otherwise.
     */
    suspend fun setCheckedTrack(info: TrackInfo?, type: Int): Boolean
}