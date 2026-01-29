/*
 * Copyright (c)  2026 Zakir Sheikh
 *
 * Created by sheik on 29 of Jan 2026
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
 * Last Modified by sheik on 29 of Jan 2026
 *
 */

package com.prime.media.console

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.SystemFacade
import com.zs.core_ui.Indication
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode as PointerNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode as CLCMN
import com.prime.media.console.RouteConsole as C

private const val TAG = "PlayerGestureHandler"

/**
 *  The current screen brightness of this activity's window.
 *
 *  This value ranges from 0.0f to 1.0f, where 0.0f is the darkest and 1.0f is the brightest.
 *  A value of -1.0f indicates that the system's default brightness is being used.
 *
 *  Note: Setting this value directly modifies the window's layout attributes.
 *  Changes may not be immediately reflected if the system is managing the brightness
 *  automatically (e.g., under automatic brightness control). In such cases, the actual
 *  brightness may be clamped or overridden by the system.
 *
 *  @see android.view.WindowManager.LayoutParams.screenBrightness
 */
private var SystemFacade.brightness: Float
    get() = (this as? Activity)?.window?.attributes?.screenBrightness ?: 1f
    set(value) {
        val window = (this as? Activity)?.window ?: return
        val attr = window.attributes
        // -1f means use system brightness.
        if (value == -1f)
            attr.screenBrightness = value
        else
        //  Confine value between 0 and 1.
            attr.screenBrightness = value.coerceIn(0f, 1f)
        window.attributes = attr
    }

/**
 * Represents the volume of the music stream, providing a normalized view (0.0 to 1.0)
 * over the system's integer volume range.
 *
 * The volume is represented as a float between 0.0 and 1.0, where 0.0 is muted and 1.0
 * is the maximum volume.  This property maps between the normalized float
 * representation and the underlying integer volume levels of the Android system's
 * `AudioManager.STREAM_MUSIC` stream.
 *
 * **Important Considerations:**
 *
 * -   **Integer Mapping:** The Android system manages volume using integer levels.
 * This property converts the 0.0-1.0 float range to the nearest integer
 * volume level when setting the volume. This means that very small
 * adjustments in the float value might not result in a change in the actual
 * system volume.
 * -   **Granularity:** The number of discrete volume levels is determined by
 * `getStreamMaxVolume(AudioManager.STREAM_MUSIC)`.  A larger maximum volume
 * means finer-grained control.
 * -   **Clamping:** When setting the volume, the provided float value is clamped
 * to the range 0.0 to 1.0 to ensure it's within valid bounds.
 *
 * @property volume Gets or sets the normalized volume of the music stream (0.0 to 1.0).
 * -   **Get:** Retrieves the current music stream volume and normalizes it to a
 * float between 0.0 and 1.0 by dividing by the maximum stream volume.
 * -   **Set:** Sets the music stream volume.  The provided float value is
 * first clamped to the 0.0-1.0 range, then converted to the corresponding
 * integer volume level.
 */
private var AudioManager.volume: Float
    get() {
        // Get the current volume of the music stream.
        val current = getStreamVolume(AudioManager.STREAM_MUSIC)
        // Get the maximum volume for the music stream.
        val max = getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        // Normalize the current volume to a float between 0.0 and 1.0.
        // Handle the case where maxVolume is 0 to avoid division by zero.
        return if (max > 0) current.toFloat() / max.toFloat() else 0f
    }
    set(value) {
        // Get the maximum volume for the music stream.
        val max = getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        // Convert the normalized volume (0.0-1.0) to an integer volume level.
        // Clamp the input value to the valid range of 0.0 to 1.0.
        val target = (value.coerceIn(0f, 1f) * max).roundToInt()
        // Log the volume change for debugging purposes.
        Log.d(TAG, "Setting stream volume to index: $target (raw value: $value, max: $max)")
        // Set the volume of the music stream.
        // The third parameter (0) is a flag that specifies whether to show a volume UI.
        setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }


fun Modifier.handlePlayerGestures(
    viewState: ConsoleViewState,
): Modifier = this then PlayerGestureHandlerElement(viewState)

private class PlayerGestureHandlerElement(
    val viewState: ConsoleViewState
) : ModifierNodeElement<PlayerGestureHandlerNode>() {

    override fun create(): PlayerGestureHandlerNode =
        PlayerGestureHandlerNode(viewState)

    override fun update(node: PlayerGestureHandlerNode) {

    }


    override fun InspectorInfo.inspectableProperties() {
        name = "playerGestureDectector"
        properties["state"] = viewState
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerGestureHandlerElement

        if (viewState != other.viewState) return false

        return true
    }

    override fun hashCode(): Int {
        return viewState.hashCode()
    }
}

private class PlayerGestureHandlerNode(
    val viewState: ConsoleViewState,
) : DelegatingNode(), PointerInputModifierNode, DrawModifierNode, CLCMN, KeyInputModifierNode {
    // TODO: Future enhancements to consider:
    //    1. Implement a distinct gesture for adjusting video scale (e.g., pinch-to-zoom).
    //    2. Improve drag scaling calculation, perhaps by using the dimensions of the drag area.
    //    3. Refine `onTap` behavior: toggle visibility only when playing; otherwise, ensure the controller is visible.
    //    4. Add support for focus and key events for devices like TVs.
    //    5. Handle left/right gestures according to the current layout direction (LTR/RTL).
    val detector = PointerNode {
        this@PlayerGestureHandlerNode.size = size
        //
        coroutineScope.launch {
            detectTapGesture()
        }

        //
        coroutineScope.launch {
            detectDragGesture()
        }
    }

    /**
     * Returns true if [offset] lies within the safe zone of this [PointerInputScope].
     *
     * The safe zone excludes a 30.dp margin from each edge.
     */
    fun PointerInputScope.isGestureInSafeZone(offset: Offset): Boolean{
        val safeInset = 30.dp.toPx()
        return  !(offset.x < safeInset ||
                offset.x > size.width - safeInset ||
                offset.y < safeInset ||
                offset.y > size.height - safeInset)
    }

    // some lateint properties
    lateinit var textMeasurer: TextMeasurer
    lateinit var style: TextStyle
    lateinit var manager: AudioManager
    lateinit var facade: SystemFacade

    var size = IntSize.Zero
    var message: TextLayoutResult? = null
        set(value) {
            field = value
            invalidateDraw()
        }

    // Returns if controller is in locked state.
    val isLocked
        get() = viewState.visibility >= C.VISIBLE_NONE_LOCKED && viewState.visibility <= C.VISIBLE_LOCKED_LOCK

    override val shouldAutoInvalidate: Boolean get() = false
    override fun onCancelPointerInput() = detector.onCancelPointerInput()
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) = detector.onPointerEvent(pointerEvent, pass, bounds)

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Currently this Modifier is always focusable and enabled.
        // We only handle "activation" keys on KeyDown to simulate a click/press action.
        // Keys supported: Enter, Center (D‑pad), NumPadEnter, and Spacebar.
        // This mirrors the behavior found in Modifier.clickable.
        // In the future, other event types or accessibility actions may be added.
        val isEnterKey = (
                event.key == Key.Enter ||
                        event.key == Key.DirectionCenter ||
                        event.key == Key.NumPadEnter ||
                        event.key == Key.Spacebar
                )

        if (event.type == KeyDown && isEnterKey) {
            // Toggle visibility when the activation key is pressed.
            toggleVisibility()
            return true // Event consumed
        }

        // Return false if the event was not handled.
        return false
    }

    override fun onPreKeyEvent(event: KeyEvent): Boolean = false

    // A simple fun that autohides the message after 3 seconds.
    var messageAutohideJob: Job? = null
    fun emit(text: String, autoHide: Boolean = true) {
        messageAutohideJob?.cancel()
        message = textMeasurer.measure(text, style)
        if (autoHide)
            messageAutohideJob = coroutineScope.launch {
                delay(2.5.seconds)
                message = null
            }
    }

    var seekMediaJob: Job? = null
    fun seekBy(mills: Long) {
        seekMediaJob?.cancel()
        seekMediaJob = coroutineScope.launch {
            emit("${mills / 1000}s", true)
            delay(200)
            viewState.seekBy(mills)
        }
    }

    // Toggles controller visibility.
    fun toggleVisibility() {
        // show hide controller
        viewState.emit(
            newVisibility = when (viewState.visibility) {
                C.VISIBLE_NONE_LOCKED -> C.VISIBLE_LOCKED_LOCK
                C.VISIBLE_LOCKED_LOCK -> C.VISIBLE_NONE_LOCKED
                C.VISIBLE -> C.VISIBLE_NONE
                C.VISIBLE_LOCKED_SEEK -> C.VISIBLE_LOCKED_SEEK
                else -> C.VISIBLE
            }
        )
    }

    @SuppressLint("SuspiciousCompositionLocalModifierRead")
    override fun onAttach() {
        super.onAttach()
        delegate(detector)
        val fontFamilyResolver = currentValueOf(LocalFontFamilyResolver)
        val density = currentValueOf(LocalDensity)
        val layoutDirection = currentValueOf(LocalLayoutDirection)
        style = currentValueOf(LocalTextStyle).copy(shadow = textShadow, color = Color.White)
        textMeasurer = TextMeasurer(fontFamilyResolver, density, layoutDirection, 8)
        facade = currentValueOf(LocalSystemFacade)
        manager = (facade as Activity).getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val textShadow = Shadow(offset = Offset(5f, 5f), blurRadius = 8.0f)
    override fun ContentDrawScope.draw() {
        drawContent()
        val msg = message ?: return

        // Measure text size
        val textWidth = msg.size.width
        val textHeight = msg.size.height

        // Position text horizontally centered
        val centerX = (size.width - textWidth) / 2f
        val topY = 40.dp.toPx()

        // Padding
        val vPadding = 4.dp.toPx()
        val hPadding = 12.dp.toPx()

        // Rect bounds
        val rectLeft = centerX - hPadding
        val rectTop = topY - vPadding
        val rectRight = centerX + textWidth + hPadding
        val rectBottom = topY + textHeight + vPadding

        val rectSize = Size(rectRight - rectLeft, rectBottom - rectTop)
        val rectTopLeft = Offset(rectLeft, rectTop)

        // Corner radius = half of rect height → pill shape
        val cornerRadius = CornerRadius(rectSize.height / 2f, rectSize.height / 2f)

        // Background rounded rect (20% alpha white)
        drawRoundRect(
            color = Color.White.copy(alpha = ContentAlpha.Indication),
            topLeft = rectTopLeft,
            size = rectSize,
            cornerRadius = cornerRadius
        )

        // Border (stroke) around rect
        drawRoundRect(
            color = Color.White,
            topLeft = rectTopLeft,
            size = rectSize,
            cornerRadius = cornerRadius,
            style = Stroke(width = Dp.Hairline.toPx())
        )

        // Text in pure white
        drawText(
            msg,
            topLeft = Offset(centerX, topY),
            color = Color.White
        )
    }

    override fun onDetach() {
        undelegate(detector)
        facade.brightness = -1f // restore to auto.
        super.onDetach()
    }


    fun onTap(count: Int = Int.MAX_VALUE) {
        Log.d(TAG, "onTap: $count")
        if (count == Int.MAX_VALUE) {
            toggleVisibility()
            return
        }
        // Hide the player controls if they are visible
        if (viewState.visibility != C.VISIBLE_NONE)
            viewState.emit(C.VISIBLE_NONE)
        val mills = count * 10 * 1_000L
        seekBy(mills)
    }

    // Backing field to store the original playback speed before a long press.
    var speed: Float = 1f
    fun onLongPress(released: Boolean) {
        return // do nothing
        Log.d(TAG, "onLongPress: $released")
        // Hide the player controls if they are visible
        if (viewState.visibility != C.VISIBLE_NONE)
            viewState.emit(C.VISIBLE_NONE)
        // When the long press starts
        if (!released) {
            // Store the current playback speed
            speed = viewState.playbackSpeed
            // Double the playback speed
            viewState.playbackSpeed = 2 * speed
            // Display ">> 2x" message without auto-hiding
            emit("⏭ 2x", false)
        } else { // When the long press is released
            // Restore the original playback speed
            viewState.playbackSpeed = speed
            emit("⏭ 1x", true)
        }
    }

    suspend fun PointerInputScope.detectTapGesture() {
        var delayJob: Job? =
            null              // Job used to delay execution for multi-tap detection.
        var lastTapMills =
            0L                  // Timestamp of the last tap (used to detect double/triple taps).
        var tapCount = 0                       // Counter for consecutive taps.
        awaitEachGesture {
            // Wait for the first pointer down event (finger touches screen) and consume it
            // so it isn’t propagated further down the gesture chain.
            val down = awaitFirstDown().also { it.consume() }
            // don't proceed if not in safe zone
            if (!isGestureInSafeZone(down.position))
                return@awaitEachGesture
            // If the screen is locked, any tap should only toggle the lock icon visibility.
            if (isLocked) {
                toggleVisibility()
                return@awaitEachGesture
            }
            // Launch a coroutine to detect a long press.
            // If the user holds down longer than the system-defined timeout,
            // trigger the onLongPress action.
            val longPressJob = coroutineScope.launch {
                delay(viewConfiguration.longPressTimeoutMillis) // Wait for long press threshold
                Log.d(TAG, "onLongPressHold: ${down.position}") // Debug log for long press position
                onLongPress(false)                              // Invoke long press callback
            }

            // Wait for the pointer to be lifted (finger up) or gesture cancellation.
            // Consume the "up" event so it doesn’t propagate further.
            val up = waitForUpOrCancellation()?.also { it.consume() }
            longPressJob.cancel()  // Cancel the long press job since the finger has been lifted or gesture ended.

            // If gesture was cancelled (e.g., another finger interrupted), exit early.
            if (up == null) // cancelled
                return@awaitEachGesture

            when {
                // Long press completed: If the pointer was held down longer than the timeout.
                up.uptimeMillis - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis -> {
                    onLongPress(true); Log.d(TAG, "onLongClick: ")
                }

                // Multi-tap: If this tap occurred within the double-tap timeout of the previous tap.
                up.uptimeMillis - lastTapMills <= viewConfiguration.doubleTapTimeoutMillis -> {
                    // This is a subsequent tap in a multi-tap sequence.
                    // Cancel any pending single tap action.
                    delayJob?.cancel()
                    ++tapCount
                    // Determine direction of seek based on tap location (left/right side of screen).
                    val times = if (down.position.x > size.width / 2) tapCount else -tapCount
                    onTap(times)
                }

                // Single tap: This is the first tap or a tap that occurred after the double-tap timeout.
                else -> {
                    // Reset tap count for a new sequence.
                    tapCount = 0
                    // Cancel any previously scheduled single tap job.
                    delayJob?.cancel()
                    // Schedule a single tap action to run after the double-tap timeout.
                    // This gives the user a chance to perform another tap for a multi-tap gesture.
                    delayJob = coroutineScope.launch {
                        delay(viewConfiguration.doubleTapTimeoutMillis)
                        Log.d(TAG, "onTap: ")
                        onTap()
                    }
                }
            }
            // Record the time of this tap for future multi-tap detection.
            lastTapMills = up.uptimeMillis
        }
    }

    suspend fun PointerInputScope.detectDragGesture() {
        var seek = 0L              // Tracks seek offset in milliseconds during drag
        var brightness = 0f        // Tracks brightness level during drag
        var volume = 0f            // Tracks volume level during drag
        var mode = 0               // Gesture mode: 0 = undecided, 1 = seek, 2 = volume, 3 = brightness
        var dragStartPosition = Offset.Unspecified //
        detectDragGestures(
            onDragStart = {
                dragStartPosition = it
                // don't proceed if not in safe zone
                if (!isGestureInSafeZone(dragStartPosition))
                    return@detectDragGestures
                // If the screen is locked, a drag should only toggle lock icon visibility.
                if (isLocked) {
                    toggleVisibility()
                    return@detectDragGestures
                }

                // Hide player controls when a drag starts.
                if (viewState.visibility != C.VISIBLE_NONE)
                    viewState.emit(C.VISIBLE_NONE)

                // Reset gesture mode to undecided.
                mode = 0
                // Store initial volume for relative adjustments during drag.
                volume = manager.volume

                // Store initial brightness. If unset (-1f), default to at least 10%
                // so dragging feels logical (avoids starting from 0% brightness).
                val value = facade.brightness
                brightness = if (value == -1f) 0.1f else value

                // Reset seek offset to 0 at drag start.
                seek = 0L
            },
            onDrag = { change, (dx, dy) ->
                // don't proceed if not in safe zone
                if (!isGestureInSafeZone(dragStartPosition))
                    return@detectDragGestures
                // Ignore drag gestures completely if the screen is locked.
                if (isLocked)
                    return@detectDragGestures
                // Consume the pointer change so it isn’t passed further down the chain.
                change.consume()
                val position = change.position
                val (width, height) = size
                // On the first drag event, determine gesture mode:
                // - Horizontal drag → SEEK
                // - Vertical drag on left half → VOLUME
                // - Vertical drag on right half → BRIGHTNESS
                if (mode == 0) {
                    val vertical =
                        abs(dy) > abs(dx)       // Check if drag is more vertical than horizontal
                    val left =
                        position.x < width / 2      // Check if drag started on left half of the screen
                    mode = when {
                        !vertical -> 1                     // Horizontal drag → SEEK mode
                        left -> 2                          // Vertical drag on left side → VOLUME mode
                        else -> 3                          // Vertical drag on right side → BRIGHTNESS mode
                    }
                }
                // Handle gesture based on detected mode
                // val reverse = this.
                when (mode) {
                    // seek
                    1 -> { // SEEK mode
                        val pct =
                            dx / width               // Convert horizontal drag distance into percentage of screen width
                        seek += (pct * 60_000).roundToLong() // Scale percentage to milliseconds (60s per full width drag)
                        Log.d(TAG, "onHorizontalDrag: $pct")
                        seekBy(seek)                       // Apply seek offset to playback
                    }
                    // right - volume
                    2 -> { // BRIGHTNESS mode
                        val pct =
                            dy / height * -1f        // Convert vertical drag distance into percentage of screen height
                        // Negative sign ensures upward drag increases brightness
                        val newBrightness = brightness + pct
                        // If brightness goes below 0, set to -1 (automatic mode), else clamp between 0% and 100%
                        brightness = if (newBrightness < 0f) -1f else newBrightness.coerceIn(0f, 1f)
                        facade.brightness = brightness     // Apply new brightness level to system
                        // Update UI message to show current brightness level
                        if (brightness == -1f)
                            emit("Ⓐ Automatic")            // Special case: automatic brightness mode
                        else
                            emit("🔆 ${(brightness * 100).roundToInt()}%")
                    }
                    // left - brightness.
                    3 -> { // VOLUME mode
                        val pct =
                            dy / height * -1f        // Convert vertical drag distance into percentage of screen height
                        // Negative sign ensures upward drag increases volume
                        volume = (volume + pct).coerceIn(0f, 1f) // Clamp volume between 0% and 100%
                        manager.volume = volume            // Apply new volume level to system
                        // Update UI message to show current volume percentage
                        emit("""🔊 ${(volume * 100).roundToInt()}%""")
                    }
                }
            }
        )
    }
}