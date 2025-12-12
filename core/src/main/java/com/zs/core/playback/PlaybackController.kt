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

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

interface PlaybackController {

    /**
     * Clears the existing [queue] and replaces it with a new queue containing the specified [values].
     * Note: The queue must only contain unique [MediaItem.mediaUri] values to ensure uniqueness.
     * so, duplicate items will be dealt with automatically.
     *
     * @param values The list of [MediaItem]s to be set in the queue.
     * @return The number of items successfully added to the queue.
     */
    suspend fun setMediaFiles(values: List<MediaFile>, index: Int = 0, position: Long = 0): Int

    /**
     * Starts playing the underlying service.
     *
     * @param playWhenReady Pass true to start playback immediately, or false to start in a paused state.
     * @see [Player.playWhenReady]
     */
    suspend fun play(playWhenReady: Boolean = true)

    /**
     * Clears the queue if loaded otherwise does nothing.
     */
    suspend fun clear()

    /** Returns the index of [MediaFile] represented by the [uri] or [Remote.INDEX_UNSET] */
    suspend fun indexOf(uri: Uri): Int
    /** Skips to [index] in queue and seeks to [mills]; returns `true` if successful. */
    suspend fun seekTo(index: Int = INDEX_UNSET, mills: Long = TIME_UNSET): Boolean

    companion object {

        const val POSITION_UNSET = C.POSITION_UNSET
        const val TIME_UNSET = C.TIME_UNSET
        const val INDEX_UNSET = C.INDEX_UNSET
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

        val PLAYLIST_RECENT = Playback.PLAYLIST_RECENT

        private const val TAG = "PlaybackController"

        /**
         * Creates and returns an instance of [PlaybackController].
         */
        operator fun invoke(context: Context): PlaybackController =
            PlaybackControllerImpl(context.applicationContext)

        /**
         * Observes NowPlaying events using a BroadcastReceiver.
         *
         * This function creates a cold flow that emits NowPlaying objects whenever the
         * AppWidgetManager.ACTION_APPWIDGET_UPDATE broadcast is received.
         *
         * @param context The context to register the receiver in.
         * @return A Flow that emits NowPlaying objects.
         */
        @Deprecated("This fun will be replaced by contrete impl. in actual impentation in PLaybackController.")
        fun observe(context: Context) = callbackFlow<NowPlaying> {
            // Create a BroadcastReceiver to listen for NowPlaying events
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    trySend(NowPlaying(intent ?: return))
                }
            }

            // Register the receiver to listen for the specified broadcast
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )

            // send signal for getting first progress
            NowPlaying.trySend(context)

            // Unregister the receiver when the flow is closed
            awaitClose {
                context.unregisterReceiver(receiver)
                Log.d(TAG, "observe: unregistering receiver")
            }
        }
    }
}