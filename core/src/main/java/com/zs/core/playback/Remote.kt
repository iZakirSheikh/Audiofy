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
import androidx.annotation.FloatRange
import androidx.media3.common.C
import androidx.media3.common.Player
import com.zs.core.db.playlists.Playlists
import kotlinx.coroutines.flow.Flow


/**
 * Represents the controller for [Playback]
 */
interface Remote {

    @SuppressLint("UnsafeOptInUsageError")
    companion object {

        const val SLEEP_TIME_UNSET = -1L

        const val POSITION_UNSET = C.POSITION_UNSET
        const val TIME_UNSET = C.TIME_UNSET

        // The standard global playlists.
        val PLAYLIST_FAVOURITE = Playlists.PRIVATE_PLAYLIST_PREFIX + "favourite"
        val PLAYLIST_RECENT = Playlists.PRIVATE_PLAYLIST_PREFIX + "recent"
        internal val PLAYLIST_QUEUE = Playlists.PRIVATE_PLAYLIST_PREFIX + "queue"

        const val REPEAT_MODE_ONE = Player.REPEAT_MODE_ONE
        const val REPEAT_MODE_ALL = Player.REPEAT_MODE_ALL
        const val REPEAT_MODE_OFF = Player.REPEAT_MODE_OFF

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

    val state: Flow<NowPlaying?>
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

    suspend fun togglePlay()

    suspend fun shuffle(shuffle: Boolean)

    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun seekTo(@FloatRange(0.0, 1.0) pct: Float)

    /** Clears the queue if loaded otherwise does nothing. */
    suspend fun clear()
}