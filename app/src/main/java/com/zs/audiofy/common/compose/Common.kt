/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 12-07-2025.
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

package com.zs.audiofy.common.compose

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zs.compose.theme.AppTheme
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

private const val TAG = "compose-common"

val edgeWidth = 10.dp

@Deprecated("Find better solution.")
private fun ContentDrawScope.drawFadedEdge(leftEdge: Boolean) {
    val edgeWidthPx = edgeWidth.toPx()
    drawRect(
        topLeft = Offset(if (leftEdge) 0f else size.width - edgeWidthPx, 0f),
        size = Size(edgeWidthPx, size.height),
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startX = if (leftEdge) 0f else size.width,
            endX = if (leftEdge) edgeWidthPx else size.width - edgeWidthPx
        ),
        blendMode = BlendMode.DstIn
    )
}

/**
 * An extension of marque that draw marqueue with faded edge.
 */
@OptIn(ExperimentalFoundationApi::class)
@Deprecated("Think more about this")
fun Modifier.marque(iterations: Int) =
    Modifier
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            drawFadedEdge(leftEdge = true)
            drawFadedEdge(leftEdge = false)
        }
        .basicMarquee(
            // Animate forever.
            iterations = iterations,
        )
        .then(this)

/**
 * A convenient [Modifier] for configuring the size and scale of a Lottie animation,
 * particularly for use as button icons.
 *
 * This modifier streamlines the creation of Lottie-based button icons by defaulting
 * to the standard Material Design icon size (24.dp) and allowing for easy scaling.
 */
inline fun Modifier.lottie(scale: Float = 1f) = this
    .requiredSize(24.dp)
    .then(Modifier.scale(scale))

/**
 * A utility class for managing and representing elapsed time, particularly for media playback.
 *
 * This class helps in tracking and updating the progress of media playback. It distinguishes
 * between time updates originating from the system (e.g., actual playback progress) and
 * updates initiated by the user (e.g., scrubbing a seek bar).
 *
 * The internal state is stored as a [MutableLongState].
 * - **Negative values**: Indicate elapsed progress as reported by the system.
 * - **Positive values**: Indicate elapsed time changes made by the user. This distinction is
 *   useful because system calls for seeking can be slow, and this helps in providing a more
 *   responsive UI for user interactions like slider adjustments.
 * - **[Long.MIN_VALUE]**: Represents an "Not Available" (N/A) or unset time.
 *
 * **Why use elapsed time instead of progress percentage?**
 * Progress can be calculated from both duration and elapsed time. However, in some media files,
 * the duration might not be available. Using elapsed time directly makes the system more robust
 * in such scenarios.
 *
 * @property state The underlying [MutableLongState] holding the raw time value.
 */
@JvmInline
value class Chronometer private constructor(private val state: MutableLongState) {
    /**
     * Constructs a [Chronometer] with an initial time value.
     *
     * @param initial The initial time value in milliseconds.
     */
    constructor(initial: Long) :
            this(mutableLongStateOf(-initial))

    /** Gets or sets the raw internal time value. */
    var raw
        get() = state.longValue
        set(value) {
            state.longValue = value
        }

    /**
     * The actual elapsed time in milliseconds, always non-negative.
     * Returns [Long.MIN_VALUE] if the raw value is [Long.MIN_VALUE] (representing N/A).
     * Otherwise, returns the absolute value of the [raw] time.
     */
    val elapsed get() = if (raw == Long.MIN_VALUE) Long.MIN_VALUE else raw.absoluteValue

    /**
     * Calculates the progress as a float value between 0.0 and 1.0.
     *
     * @param duration The total duration of the media in milliseconds.
     * @return The progress as a float.
     *         - Returns `1.0f` if the `duration` is [Remote.TIME_UNSET] (i.e., unknown or indefinite).
     *         - Returns `0.0f` if [elapsed] is [Long.MIN_VALUE] (N/A) or `duration` is zero or negative.
     *         - Otherwise, calculates `elapsed / duration`.
     */
    @Composable
    fun progress(duration: Long): Float {
        // If duration is not set or invalid, or elapsed time is N/A,
        // it's not possible to calculate meaningful progress.
        val progress =  when {
            elapsed == 0L -> 0f // if nothing has elapsed; just set it to
            duration == Remote.TIME_UNSET -> 1.0f // Treat as fully progressed if duration is unknown.
            else -> (elapsed.toFloat() / duration.toFloat()).coerceIn(0.0f, 1.0f) // Standard progress calculation.
        }
        // if user scroller; don't animate.
        if (raw > 0)
            return progress
        val ms = AppTheme.motionScheme
        val newProgress by animateFloatAsState(progress, ms.fastSpatialSpec())
        return newProgress
    }

    /**
     * Calculates the remaining time in milliseconds.
     *
     * @param duration The total duration of the media in milliseconds.
     * @return The remaining time in milliseconds.
     *         - Returns [Long.MIN_VALUE] if `duration` is [Remote.TIME_UNSET] (unknown or indefinite).
     *         - Returns [Long.MIN_VALUE] if [elapsed] is [Long.MIN_VALUE] (N/A) or `duration` is zero or negative.
     *         - Otherwise, calculates `duration - elapsed`.
     */
    fun remaining(duration: Long): Long {
        return when {
            // If duration is unknown, remaining time is also unknown.
            duration == Remote.TIME_UNSET -> Long.MIN_VALUE
            // If elapsed time is N/A or duration is invalid, remaining time is unknown.
            elapsed == Long.MIN_VALUE || duration <= 0L -> Long.MIN_VALUE
            // Standard calculation for remaining time.
            else -> duration - elapsed
        }
    }
}

/**
 * A Composable extension property that provides a [Chronometer] instance
 * synchronized with the [NowPlaying] state.
 *
 * This property creates and remembers a [Chronometer] that reflects the playback
 * position of the current media item. It uses [LaunchedEffect] to observe
 * changes in the [NowPlaying] object and update the [Chronometer] accordingly.
 *
 * The [Chronometer] updates its internal time based on the `position`, `duration`,
 * `playing`, `timeStamp`, and `speed` properties of the [NowPlaying] object.
 *
 * - If playback is not active (`!playing`), or if the position or duration is unset,
 *   the [Chronometer] will not actively update.
 * - When playback is active, the [Chronometer] considers the time elapsed since
 *   the `timeStamp` and the `speed` of playback to provide an accurate current position.
 * - It launches a coroutine to periodically update the [Chronometer]'s raw value
 *   every second while playback is active and the user hasn't manually adjusted
 *   the playback position (indicated by `chronometer.raw > 0`).
 *
 * @return A [Chronometer] instance that tracks the playback progress.
 */
val NowPlaying.chronometer: Chronometer
    @Composable
    get() {
        // Create and remember a Chronometer instance, initialized with the current position.
        // This ensures the chronometer state persists across recompositions.
        val chronometer = remember { Chronometer(0L) }

        // Restart this effect whenever the NowPlaying object changes.
        LaunchedEffect(this) {
            Log.d(TAG, "NowPlaying: ${this@chronometer}")
            // Time since NowPlaying was last updated.
            val elapsedSinceLastUpdate = System.currentTimeMillis() - timeStamp

            // Estimate the current position by adjusting for elapsed time and speed.
            // Assumes speed has remained constant since the timestamp.
            var current = position + (elapsedSinceLastUpdate * speed).roundToLong()
            // Initialize the chronometer's raw value.
            // It's set to the negative of the `current` calculated position.
            // Negative values in `Chronometer` signify system-reported progress.
            // Use 0 if position is unknown; otherwise, store as a negative value.
            chronometer.raw = if (position == Remote.TIME_UNSET || state < Remote.PLAYER_STATE_READY) 0 else -current
            // Exit early if:
            // 1. The position is unknown,
            // 2. Playback has completed,
            // 3. Playback is paused/stopped.
            if (position == Remote.TIME_UNSET || !playing)
                return@LaunchedEffect
            // Continuously update the chronometer while playing,
            // unless the user has overridden the progress (i.e., raw becomes positive).
            while (true) {
                // Delay for 1 second before the next update.
                delay(1000)
                // Log the current state for debugging.
                Log.d(TAG, "duration: $duration | raw: ${chronometer.raw} | position: $current")
                // Stop ticking if:
                // User manually scrubs (raw > 0), or
                if (chronometer.raw > 0) break
                // reset duration if still playing
                if (duration != Remote.TIME_UNSET && current >= duration)
                    current = current - duration
                // Advance the `current` position by 1 second, adjusted for playback `speed`.
                current += (1000L * speed).roundToLong()
                // Update the chronometer's raw value with the new system-reported progress.
                // Again, the value is negative to indicate it's a system update.
                chronometer.raw = -current
            }
        }
        return chronometer
    }