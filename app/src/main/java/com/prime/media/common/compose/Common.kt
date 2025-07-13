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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
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


val NowPlaying.chronometer: Long
    @Composable
    get() {
        if (this.position == Remote.TIME_UNSET)
            return Remote.TIME_UNSET
        if (!playing)
            return position
        var chronometer by remember { mutableLongStateOf(this.position) }
        LaunchedEffect( this) {
            // since we might have moved from old to new time
            // FIXME - consider the case if the user have set variable speed during the time.
            val elapsed = System.currentTimeMillis() - timeStamp
            chronometer += (elapsed * speed).roundToLong()
            var current = position
            // Launch a coroutine to update the chronometer periodically.
            launch {
                // Continue updating the chronometer until the current position reaches the duration.
                while (chronometer < duration) {
                    Log.d(
                        TAG,
                        "duration: $duration | value: ${chronometer} | position: $current "
                    )
                    // Delay for 1 second.
                    delay(1000)
                    // Update the current position based on the playback speed.
                    current += (1000 * speed).roundToLong()
                    chronometer = current
                }
            }

        }
        return chronometer
    }