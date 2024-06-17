/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 22-06-2024.
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

package com.prime.media.core.compose.shimmer

import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val TAG = "Highlight"

/**
 * A Modifier that provides a visual highlight effect by animating a circular
 * outline around the Composable. This effect is designed todraw the user's
 * attention to the highlighted element.
 *
 * The highlight starts as a small circle at the center of the Composable and
 * expands to the full size of the element, creating a visual "pulse".
 *
 * @param color The color of the highlighter. Defaults to [Color.White].
 * @param width The width (stroke thickness) of the highlight circle. Defaults to 3.dp.
 * @param animationSpec The animation specification for the highlight expansion.
 * Defaults to a [tween] animation with a duration of 750ms and a delay of 1000ms.
 */
fun Modifier.pulsate(
    color: Color = Color.White,
    width: Dp = 3.dp,
    animationSpec: DurationBasedAnimationSpec<Float> = tween(750, 2000)
) = this then Modifier.composed {
    // Animates the highlight effect using an infinite transition.
    // TODO - Consider using the Animation API in the future for:
    //  1. Compatibility with Modifier.Node (when migrated)
    //  2. Easier implementation of features like repeat count.
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Defines the animation for the shimmer, including duration, delay, and easing.
            animation = animationSpec,
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "highlight progress"
    )

    // Draws the highlight effect on the content.
    drawWithContent {
        drawContent()
        val strokeWidthPx = width.toPx()
        val (widthPx, heightPx) = size
        val center = size.center
        // Calculates the radius of the highlight circle based on animation progress.
        val radius = ((kotlin.math.min(widthPx, heightPx) / 2) * progress)
        // Draws the highlight circle with adjusted alpha based on progress.
        // TODO: Enhance the visual effect by using a gradient instead of a solid color.
        //       The gradient should run from the middle of the stroke outwards.
        drawCircle(color.copy(1- progress), style = Stroke(strokeWidthPx), radius = radius)
    }
}