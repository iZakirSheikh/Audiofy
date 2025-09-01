/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
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

package com.zs.audiofy.console

import android.net.Uri
import androidx.annotation.FloatRange
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.ConstraintSetScope
import androidx.constraintlayout.compose.Visibility
import com.zs.audiofy.common.Route
import com.zs.core.playback.MediaFile
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.VideoProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

object RouteConsole : Route {
    // Component IDs
    const val ID_PLAYING_INDICATOR = "_playing_indicator"
    const val ID_BTN_COLLAPSE = "_btn_collapse"
    const val ID_ARTWORK = "_artwork"
    const val ID_TITLE = "_title"
    const val ID_SUBTITLE = "_subtitle"
    const val ID_EXTRA_INFO = "_extra_info"
    const val ID_SHUFFLE = "_shuffle"
    const val ID_BTN_REPEAT_MODE = "_btn_repeat_mode"
    const val ID_BTN_SKIP_PREVIOUS = "_btn_skip_previous"
    const val ID_BTN_PLAY_PAUSE = "_play_pause"
    const val ID_BTN_SKIP_TO_NEXT = "_skip_next"
    const val ID_SEEK_BAR = "_seek_bar"
    const val ID_VIDEO_SURFACE = "_video_surface"
    const val ID_MESSAGE = "_message"
    const val ID_BACKGROUND = "_background"
    const val ID_SCRIM = "_scrim"
    const val ID_BTN_RESIZE_MODE = "_resize_mode"
    const val ID_BTN_ROTATION_LOCK = "_rotation_lock"
    const val ID_BTN_QUEUE = "_queue"
    const val ID_BTN_SLEEP_TIMER = "_sleep_timer"
    const val ID_BTN_PLAYBACK_SPEED = "_playback_speed"
    const val ID_BTN_EQUALIZER = "_equalizer"
    const val ID_BTN_MEDIA_INFO = "_media_info"
    const val ID_BTN_LIKED = "_liked"
    const val ID_BTN_MORE = "_more"
    const val ID_BTN_LOCK = "_lock"
    const val ID_CAPTIONS = "_captions"
    const val ID_BANNER_AD = "_banner_ad"

    const val VISIBILITY_AUTO_HIDE_DELAY = 5_000L
    const val MESSAGE_AUTO_HIDE_DELAY = 5_000L

    const val VISIBILITY_INVISIBLE = 0
    const val VISIBILITY_INVISIBLE_LOCKED = 1
    const val VISIBILITY_VISIBLE_LOCK = 2
    const val VISIBILITY_VISIBLE_SEEK = 3
    const val VISIBILITY_VISIBLE = 4
}

abstract class Constraints(val titleTextSize: Int) {

    abstract val constraints: ConstraintSet

    protected val COLLAPSE = ConstrainedLayoutReference(RouteConsole.ID_BTN_COLLAPSE)
    protected val ARTWORK = ConstrainedLayoutReference(RouteConsole.ID_ARTWORK)
    protected val TITLE = ConstrainedLayoutReference(RouteConsole.ID_TITLE)
    protected val SUBTITLE = ConstrainedLayoutReference(RouteConsole.ID_SUBTITLE)
    protected val EXTRA_INFO = ConstrainedLayoutReference(RouteConsole.ID_EXTRA_INFO)
    protected val SHUFFLE = ConstrainedLayoutReference(RouteConsole.ID_SHUFFLE)
    protected val REPEAT_MODE = ConstrainedLayoutReference(RouteConsole.ID_BTN_REPEAT_MODE)
    protected val SKIP_PREVIOUS = ConstrainedLayoutReference(RouteConsole.ID_BTN_SKIP_PREVIOUS)
    protected val PLAY_PAUSE = ConstrainedLayoutReference(RouteConsole.ID_BTN_PLAY_PAUSE)
    protected val SKIP_TO_NEXT = ConstrainedLayoutReference(RouteConsole.ID_BTN_SKIP_TO_NEXT)
    protected val SEEK_BAR = ConstrainedLayoutReference(RouteConsole.ID_SEEK_BAR)
    protected val VIDEO_SURFACE = ConstrainedLayoutReference(RouteConsole.ID_VIDEO_SURFACE)
    protected val TOAST = ConstrainedLayoutReference(RouteConsole.ID_MESSAGE)
    protected val BACKGROUND = ConstrainedLayoutReference(RouteConsole.ID_BACKGROUND)
    protected val SCRIM = ConstrainedLayoutReference(RouteConsole.ID_SCRIM)
    protected val RESIZE_MODE = ConstrainedLayoutReference(RouteConsole.ID_BTN_RESIZE_MODE)
    protected val ROTATION_LOCK = ConstrainedLayoutReference(RouteConsole.ID_BTN_ROTATION_LOCK)
    protected val QUEUE = ConstrainedLayoutReference(RouteConsole.ID_BTN_QUEUE)
    protected val SLEEP_TIMER = ConstrainedLayoutReference(RouteConsole.ID_BTN_SLEEP_TIMER)
    protected val SPEED = ConstrainedLayoutReference(RouteConsole.ID_BTN_PLAYBACK_SPEED)
    protected val LIKED = ConstrainedLayoutReference(RouteConsole.ID_BTN_LIKED)
    protected val MORE = ConstrainedLayoutReference(RouteConsole.ID_BTN_MORE)
    protected val EQUALIZER = ConstrainedLayoutReference(RouteConsole.ID_BTN_EQUALIZER)
    protected val INFO = ConstrainedLayoutReference(RouteConsole.ID_BTN_MEDIA_INFO)
    protected val INDICATOR = ConstrainedLayoutReference(RouteConsole.ID_PLAYING_INDICATOR)
    protected val LOCK = ConstrainedLayoutReference(RouteConsole.ID_BTN_LOCK)


    private fun ConstraintSetScope.hide(ref: ConstrainedLayoutReference) {
        constrain(ref) {
            visibility = Visibility.Invisible
        }
    }

    /**
     * Makes every component invisible [except] these.
     */
    fun ConstraintSetScope.hideController(except: Array<String>) {
        val hideAll = except.isEmpty()

        if (hideAll || !except.contains(RouteConsole.ID_BTN_COLLAPSE)) hide(COLLAPSE)
        if (hideAll || !except.contains(RouteConsole.ID_SUBTITLE)) hide(SUBTITLE)
        if (hideAll || !except.contains(RouteConsole.ID_TITLE)) hide(TITLE)
        if (hideAll || !except.contains(RouteConsole.ID_EXTRA_INFO)) hide(EXTRA_INFO)
        if (hideAll || !except.contains(RouteConsole.ID_SHUFFLE)) hide(SHUFFLE)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_REPEAT_MODE)) hide(REPEAT_MODE)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_SKIP_PREVIOUS)) hide(SKIP_PREVIOUS)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_PLAY_PAUSE)) hide(PLAY_PAUSE)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_SKIP_TO_NEXT)) hide(SKIP_TO_NEXT)
        if (hideAll || !except.contains(RouteConsole.ID_SEEK_BAR)) hide(SEEK_BAR)
        if (hideAll || !except.contains(RouteConsole.ID_MESSAGE)) hide(TOAST)
        if (hideAll || !except.contains(RouteConsole.ID_SCRIM)) hide(SCRIM)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_RESIZE_MODE)) hide(RESIZE_MODE)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_ROTATION_LOCK)) hide(ROTATION_LOCK)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_QUEUE)) hide(QUEUE)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_SLEEP_TIMER)) hide(SLEEP_TIMER)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_PLAYBACK_SPEED)) hide(SPEED)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_LIKED)) hide(LIKED)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_MORE)) hide(MORE)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_EQUALIZER)) hide(EQUALIZER)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_MEDIA_INFO)) hide(INFO)
        if (hideAll || !except.contains(RouteConsole.ID_PLAYING_INDICATOR)) hide(INDICATOR)
        if (hideAll || !except.contains(RouteConsole.ID_BTN_LOCK)) hide(LOCK)
    }
}

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
interface QueueViewState {

    val state: StateFlow<NowPlaying?>
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

interface ConsoleViewState: QueueViewState {

    val provider: VideoProvider

    @get:FloatRange(from = 0.25, to = 3.0)
    @set:FloatRange(from = 0.25, to = 3.0)
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
     * A message to be shown to the user for a limited time.
     * A null value means no message is set.
     * A non-null value is reset to null after [DEFAULT_MESSAGE_TIME_OUT] ms.
     *
     * @property message The message to display or null to hide it.
     * @see DEFAULT_MESSAGE_TIME_OUT
     */
    var message: CharSequence?

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

    fun sleepAt(mills: Long)
}