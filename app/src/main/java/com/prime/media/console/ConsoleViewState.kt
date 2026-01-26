/*
 * Copyright (c)  2026 Zakir Sheikh
 *
 * Created by sheik on 21 of Jan 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last Modified by sheik on 21 of Jan 2026
 *
 */

package com.prime.media.console

import android.app.Activity
import android.net.Uri
import androidx.annotation.FloatRange
import androidx.compose.runtime.Stable
import com.zs.core.playback.MediaFile
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.NowPlaying2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the state of the queue.
 * This interface defines the contract for accessing and manipulating the playback queue.
 *
 * @property state A [StateFlow] that emits the currently playing [NowPlaying] object,
 * or `null` if nothing is playing.
 *
 * @property queue A [Flow] that emits a list of [MediaFile] objects representing the
 * current playback queue.
 *  - `null` indicates that the queue is currently being loaded.
 *  - An empty list indicates that the queue is empty.
 *  - A non-empty list represents the loaded queue.
 */
@Stable
interface QueueViewState {

    val state: StateFlow<NowPlaying2?>
    val queue: Flow<List<MediaFile>?>

    /**
     * Play the track of the queue identified by the [uri]
     */
    fun skipTo(key: Uri)

    /**
     * Remove the track from the queue identified by [key].
     */
    fun remove(key: Uri)

    /**
     * Delete the track device and then removes from the queue
     */
    fun delete(key: Uri, resolver: Activity)

    /**
     * Toggle the like state of the track identified by [uri]. if null toggles like of current.
     */
    fun toggleLike(uri: Uri? = null)

    fun cycleRepeatMode()

    /**
     * Clears the whole queue.
     */
    fun clear()

    fun shuffle(enable: Boolean)
}

@Stable
interface ConsoleViewState: QueueViewState {

    // val cues: Flow<String?>

    @setparam:FloatRange(from = 0.25, to = 5.0)
    var playbackSpeed: Float

    /**
     * Represents the visibility state of the UI controls on the console screen.
     * It can take one of the following values:
     * - [RouteConsole.VISIBILITY_INVISIBLE]: All controls are hidden.
     * - [RouteConsole.VISIBILITY_VISIBLE]: All controls are visible.
     * - [RouteConsole.VISIBILITY_VISIBLE_LOCKED]: Controls are visible and locked, preventing auto-hide.
     * - [RouteConsole.VISIBILITY_VISIBLE_SEEK]: Controls are visible, typically during a seek operation.
     */
    val visibility: Int

    /**
     * Emits a new visibility state for the player controls.
     *
     * @param visible An array of strings representing the IDs of the UI elements to be shown.
     *                - `null`: Show all default controls.
     *                - Empty array: Hide all controls.
     *                - Array with IDs: Show only the specified controls.
     * @param timeout The duration in milliseconds after which the controls visibility should revert
     *                to the default state. A value of -1 means the visibility change is permanent
     *                until the next call to `emit`.
     */
    fun emit(newVisibility: Int, delayed: Boolean = false)

    fun skipToNext()
    fun skipToPrev()
    fun togglePlay()
    fun seekTo(pct: Float)
    fun seekBy(mills: Long)

    fun sleepAt(mills: Long)

    //fun getVideoProvider(): VideoProvider
    // suspend fun getAvailableTracks(type: Int): List<TrackInfo>
    // suspend fun getCheckedTrack(type: Int): TrackInfo?
    // fun setCheckedTrack(type: Int, info: TrackInfo?)

    suspend fun getPlaybackState(): Int
    suspend fun getBufferedPct(): Float
}