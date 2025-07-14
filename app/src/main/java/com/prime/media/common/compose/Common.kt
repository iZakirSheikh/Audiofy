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

package com.prime.media.common.compose

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
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
    .size(24.dp)
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
     * @param initial The initial time value in milliseconds. This value will be stored according
     *                to the rules defined for the [raw] property setter (i.e., user-set values
     *                become positive, system-set negative).
     */
    constructor(initial: Long) : this(mutableLongStateOf(if (initial == Remote.TIME_UNSET) Long.MIN_VALUE else -initial))

    /**
     * Gets or sets the raw internal time value.
     *
     * **Getter**: Returns the stored raw long value.
     *
     * **Setter**:
     * - If `value` is [Long.MIN_VALUE], it's stored as is.
     * - If `value` is negative (typically a system-reported progress like [NowPlaying.chronometer]),
     *   it's stored as is (negative).
     * - If `value` is positive (typically a user-initiated change, e.g., from a slider),
     *   it's stored as its negative equivalent. This internal representation helps distinguish
     *   user actions from system updates.
     */
    var raw
        get() = state.longValue
        set(value) {
            state.longValue = when {
                // Retain Long.MIN_VALUE as is, as it signifies N/A.
                value == Long.MIN_VALUE -> value
                // If the value is already negative (system update), store it as is.
                value < 0 -> value
                // If the value is positive (user update), store its negative counterpart.
                // This ensures user-initiated changes are marked differently internally.
                else -> -value
            }
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
    fun progress(duration: Long): Float {
        // If duration is not set or invalid, or elapsed time is N/A,
        // it's not possible to calculate meaningful progress.
        return when {
            duration == Remote.TIME_UNSET -> 1.0f // Treat as fully progressed if duration is unknown.
            elapsed == Long.MIN_VALUE || duration <= 0L -> 0.0f // No progress if elapsed is N/A or duration is invalid.
            else -> (elapsed.toFloat() / duration.toFloat()).coerceIn(
                0.0f,
                1.0f
            ) // Standard progress calculation.
        }
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
        val chronometer = remember { Chronometer(position) }

        // Launch an effect that updates the chronometer based on changes in the NowPlaying state.
        // This effect will re-launch whenever the `NowPlaying` object instance changes.
        LaunchedEffect(this) {
            // Exit the effect early if essential playback information is missing or playback is inactive.
            // - `position == Remote.TIME_UNSET`: The current playback position is unknown.
            // - `position == duration`: Playback has reached the end.
            // - `!playing`: Playback is currently paused or stopped.
            // In these cases, the chronometer should not be actively ticking.
            if (position == Remote.TIME_UNSET || position == duration || !playing)
                return@LaunchedEffect
            // Calculate the elapsed time since the `NowPlaying` state was last updated (`timeStamp`).
            // This is important to account for any delay between when the `NowPlaying` object
            // was created/updated and when this `LaunchedEffect` block is executed.
            // It also factors in the playback `speed`.
            val elapsedSinceLastUpdate = System.currentTimeMillis() - timeStamp

            // Calculate the current estimated playback position.
            // This starts with the `position` reported in `NowPlaying` and adds the
            // calculated `elapsedSinceLastUpdate` adjusted by the playback `speed`.
            // This gives a more accurate current position than relying solely on `NowPlaying.position`,
            // especially if updates are infrequent or playback speed is not 1.0.
            // FIXME: The calculation of 'current' might need refinement if the playback 'speed'
            // can change dynamically and frequently between `NowPlaying` updates.
            // This calculation assumes `speed` is constant since `timeStamp`.
            var current = position + (elapsedSinceLastUpdate * speed).roundToLong()

            // Initialize the chronometer's raw value.
            // It's set to the negative of the `current` calculated position.
            // Negative values in `Chronometer` signify system-reported progress.
            chronometer.raw = -current
            // Launch a new coroutine to handle the continuous ticking of the chronometer.
            // This coroutine will run as long as `playing` is true.
            while (playing) {
                // Log the current state for debugging.
                Log.d(TAG, "duration: $duration | raw: ${chronometer.raw} | position: $current")
                // Delay for 1 second before the next update.
                delay(1000)
                // Check if the chronometer's raw value is positive.
                // A positive value indicates that the user has manually interacted with
                // the chronometer (e.g., by scrubbing a seek bar).
                // If so, stop this automatic ticking loop to avoid overriding the user's input.
                // The loop will effectively pause until the next `NowPlaying` state change
                // re-evaluates the `LaunchedEffect`.
                if (chronometer.raw > 0) break

                // Advance the `current` position by 1 second, adjusted for playback `speed`.
                current += (1000L * speed).roundToLong()

                // Update the chronometer's raw value with the new system-reported progress.
                // Again, the value is negative to indicate it's a system update.
                chronometer.raw = -current

            }
        }
        return chronometer
    }