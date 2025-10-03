/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on 30=09-2025.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.console


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zs.audiofy.common.SystemFacade
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.compose.theme.text.LocalTextStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode as CLCMN
import com.zs.audiofy.console.RouteConsole as C

private const val TAG = "PlayerGestureDetector"

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


fun Modifier.detectPlayerGestures(
    viewState: ConsoleViewState,
    onRequestSeek: (pct: Float) -> Unit
): Modifier = this then PlayerGestureDetectorElement(viewState, onRequestSeek)

private class PlayerGestureDetectorElement(
    val viewState: ConsoleViewState,
    val onRequestSeek: (Float) -> Unit
) : ModifierNodeElement<PlayerGestureDetectorNode>() {

    override fun create(): PlayerGestureDetectorNode =
        PlayerGestureDetectorNode(viewState, onRequestSeek)

    override fun update(node: PlayerGestureDetectorNode) {

    }

    override fun InspectorInfo.inspectableProperties() {
        name = "playerGestureDectector"
        properties["state"] = viewState
        properties["onSeekRequest"] = onRequestSeek
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerGestureDetectorElement

        if (viewState != other.viewState) return false
        if (onRequestSeek != other.onRequestSeek) return false

        return true
    }

    override fun hashCode(): Int {
        var result = viewState.hashCode()
        result = 31 * result + onRequestSeek.hashCode()
        return result
    }
}

private class PlayerGestureDetectorNode(
    val viewState: ConsoleViewState,
    val onRequestSeek: (Float) -> Unit,
) : DelegatingNode(), PointerInputModifierNode, DrawModifierNode, CLCMN {
    override val shouldAutoInvalidate: Boolean
        get() = false

    var messageJob: Job? = null
    var message: String? = null
        set(value) {
            // TODO - Add animation
            messageJob?.cancel()
            messageJob = coroutineScope.launch {
                field = value
                invalidateDraw()
                delay(2_500)
                field = null
                invalidateDraw()
            }
        }

    var seekByJob: Job? = null
    fun seekBy(count: Int) {
        seekByJob?.cancel()
        seekByJob = coroutineScope.launch {
            delay(100)
            viewState.seek(count * 1_000L)
            message = "${count * 10L}s"
        }
    }

    lateinit var textMeasurer: TextMeasurer
    lateinit var style: TextStyle
    lateinit var manager: AudioManager
    lateinit var facade: SystemFacade


    val textShadow = Shadow(offset = Offset(5f, 5f), blurRadius = 8.0f)
    override fun ContentDrawScope.draw() {
        drawContent()
        Log.d(TAG, "draw: invalidating")
        val msg = message ?: return
        drawText(
            textMeasurer,
            msg,
            style = style.copy(
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                shadow = textShadow
            ),
            topLeft = Offset(size.width / 2 - 10.dp.toPx(), 70.dp.toPx())
        )
    }

    val pointerInputModifierNode = SuspendingPointerInputModifierNode({ onNewEvent() })

    override fun onCancelPointerInput() = pointerInputModifierNode.onCancelPointerInput()
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) = pointerInputModifierNode.onPointerEvent(pointerEvent, pass, bounds)

    @SuppressLint("SuspiciousCompositionLocalModifierRead")
    override fun onAttach() {
        super.onAttach()
        delegate(pointerInputModifierNode)
        val fontFamilyResolver = currentValueOf(LocalFontFamilyResolver)
        val density = currentValueOf(LocalDensity)
        val layoutDirection = currentValueOf(LocalLayoutDirection)
        style = currentValueOf(LocalTextStyle)
        textMeasurer = TextMeasurer(fontFamilyResolver, density, layoutDirection, 8)
        facade = currentValueOf(LocalSystemFacade)
        manager = (facade as Activity).getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onDetach() {
        undelegate(pointerInputModifierNode)
        facade.brightness = -1f
        super.onDetach()
    }

    val enabled get() = viewState.visibility != C.VISIBILITY_INVISIBLE_LOCKED && viewState.visibility != C.VISIBILITY_VISIBLE_LOCK

    fun toggleVisibility() {
        // show hide controller
        viewState.emit(
            newVisibility = when (viewState.visibility) {
                C.VISIBILITY_VISIBLE -> C.VISIBILITY_INVISIBLE
                C.VISIBILITY_INVISIBLE -> C.VISIBILITY_VISIBLE
                C.VISIBILITY_INVISIBLE_LOCKED -> C.VISIBILITY_VISIBLE_LOCK
                else -> C.VISIBILITY_INVISIBLE_LOCKED
            }
        )
    }

    fun PointerInputScope.onNewEvent() {
        // TODO - Handle this manually to detect long press hold release as well.
        var lastTapMills = 0L
        var tapCount = 0
        coroutineScope.launch {
            detectTapGestures { (x, y) ->
                val now = System.currentTimeMillis()
                when {
                    // Check if this tap is within the "double-tap timeout" from the previous tap
                    now - lastTapMills <= viewConfiguration.doubleTapTimeoutMillis -> {
                        if (!enabled)
                            return@detectTapGestures
                        tapCount++      // consecutive tap
                        viewState.emit(C.VISIBILITY_INVISIBLE)
                        val times = if (x > size.width / 2) tapCount else -tapCount
                        seekBy(times)
                    }

                    else -> {
                        tapCount = 0    // new tap sequence
                        toggleVisibility()
                    }
                }
                lastTapMills = now
            }
        }
        // These are used to keep track of the brightness/volume to make change more
        // clean.
        var volume = manager.volume
        var brightness = facade.brightness
        coroutineScope.launch {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    if (!enabled) {
                        // Show message and return on lock
                        toggleVisibility()
                        return@detectVerticalDragGestures
                    }
                    if (viewState.visibility != C.VISIBILITY_INVISIBLE)
                        viewState.visibility == C.VISIBILITY_INVISIBLE
                    val (width, _) = size
                    // Get the position of the gesture
                    val positionX = change.position.x
                    // Calculate the change in volume or brightness based on the drag amount.
                    // The dragAmount is in pixels, so we convert it to a normalized value
                    // The scaling factor
                    // (-0.001f) was derived empirically.
                    val real = (dragAmount / 1.dp.toPx()) * -0.001f // scale factor

                    // Check if the drag gesture is on the left side of the screen.
                    if (positionX < width / 2) {
                        // Brightness Control
                        // -------------------
                        val new = (brightness + real)
                        // Adjust brightness.  If the user drags downwards and the brightness is
                        // already at its minimum (0f), allow it to go to -1f (automatic).
                        brightness =
                            if (new < 0f && real < 0f) -1f else new.coerceIn(0f, 1f)
                        facade.brightness =
                            brightness           // Set the system brightness.
                        // Update the UI message to display the current brightness level.
                        if (brightness == -1f)
                            message = "Ⓐ Automatic"
                        else
                            message = """🔆 ${(brightness * 100).roundToInt()}%"""
                    } else {
                        //  Volume Control
                        //  ----------------
                        // Calculate the new volume.
                        volume = (volume + real).coerceIn(
                            0f,
                            1f
                        ) // Keep volume within 0-1 range.
                        manager.volume =
                            volume                   // Set the system volume.
                        // Update the UI message to display the current volume percentage.
                        message = """🔊 ${(volume * 100).roundToInt()}%"""
                    }
                    // Mark the gesture as consumed, so other gestures
                    // don't also respond to it.
                    change.consume()
                }
            )
        }
    }
}

