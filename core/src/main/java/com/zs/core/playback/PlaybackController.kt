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
import android.util.Log
import androidx.core.content.ContextCompat
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
    suspend fun setMediaFiles(values: List<MediaFile>): Int

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

    /** Ensures that controller is connected with service. */
    suspend fun connect()

    companion object {

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