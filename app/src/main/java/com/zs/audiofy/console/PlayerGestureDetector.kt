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


import android.util.Log
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.zs.audiofy.console.RouteConsole as C

private const val MAX_INTERVAL_BETWEEN_CONTINUOUS_TAPS = 300L

private const val TAG = "PlayerGestureDetector"


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

fun Modifier.detectPlayerGestures(
    viewState: ConsoleViewState,
    onRequestSeek: (pct: Float) -> Unit
): Modifier = this then PlayerGestureDetectorElement(viewState, onRequestSeek)


class PlayerGestureDetectorNode(
    val viewState: ConsoleViewState,
    onRequestSeek: (Float) -> Unit,
) : DelegatingNode(), PointerInputModifierNode, DrawModifierNode {


    override fun onCancelPointerInput() = pointerInputNode.onCancelPointerInput()
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) = pointerInputNode.onPointerEvent(pointerEvent, pass, bounds)
    private var lastMessage: String? = null

    private var offset = Offset.Unspecified



    var tapJob: Job? = null
    val pointerInputNode = delegate(
        SuspendingPointerInputModifierNode {
            var lastTapMills = 0L
            var tapCount = 0
            coroutineScope.launch {
                detectTapGestures {offset ->
                    val now = System.currentTimeMillis()

                    // Check if this tap is within the "double-tap timeout" from the previous tap
                    if (now - lastTapMills <= viewConfiguration.doubleTapTimeoutMillis) {
                        tapCount++      // consecutive tap
                        viewState.emit(C.VISIBILITY_INVISIBLE)
                        this@PlayerGestureDetectorNode.offset = offset
                        seekBy(tapCount)
                    } else {
                        this@PlayerGestureDetectorNode.offset = Offset.Unspecified
                        tapCount = 0    // new tap sequence
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

                    lastTapMills = now
                }
            }
            coroutineScope.launch {
                // Never reached
                detectDragGestures { _, _ -> lastMessage = "Dragging" }
            }

        }
    )

    var seekByJob: Job? = null
    fun seekBy(times: Int){
        seekByJob?.cancel()
        lastMessage = null
        seekByJob = coroutineScope.launch {
            delay(100)
            viewState.seek(times * 1_000L)
            lastMessage = "${times * 1_000L}"
            invalidateDraw()
        }
    }


    override fun ContentDrawScope.draw() {
        drawContent()
        Log.d(TAG, "draw: invalidating")
        if (offset.isSpecified)
            lastMessage?.let { msg ->
                drawContext.canvas
                    .nativeCanvas.drawText(
                    msg,
                    offset.x,
                    offset.y - 20.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 32.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
    }
}