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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.annotations.ApiStatus.Experimental

private val DefaultShimmerColor = Color.Gray
private val DefaultShimmerColors = listOf(Color.Transparent, DefaultShimmerColor, Color.Transparent)
private val DefaultShimmerWidth = 30.dp
private val DefaultShimmerAnimationSpec = tween<Float>(1000, 2000, LinearEasing)

private const val TAG = "Shimmer"

/**
 * Applies a shimmer effect to this Modifier. The shimmer effect is a gradient that
 * moves across the surface, creating a shimmering appearance.
 *
 * The shimmer effect is animated by default using a [DurationBasedAnimationSpec].
 * You can customize the animation using the `animationSpec` parameter.
 *
 * Note: This function is experimental and might change in the future. It is planned to be
 * re-written using theModifier.Node API.
 *
 * @param colors The colors of the shimmer effect. Defaults to [DefaultShimmerColors].
 * @param width The width of the shimmer effect in Dp. Defaults to [DefaultShimmerWidth].
 * @param blendMode The blend mode to use for drawing the shimmer effect. Defaults to [BlendMode.Hardlight].
 * @param animationSpec The animation spec to use for the shimmer effect. Defaults to [DefaultShimmerAnimationSpec].
 */
@Experimental
fun Modifier.shimmer(
    colors: List<Color> = DefaultShimmerColors,
    width: Dp = DefaultShimmerWidth,
    blendMode: BlendMode = BlendMode.Hardlight,
    animationSpec: DurationBasedAnimationSpec<Float> = DefaultShimmerAnimationSpec
) = this then Modifier.composed {
    // Creates an infinite animation loop for the shimmer effect.
    val transition = rememberInfiniteTransition(label = "shimmer")

    // Animates the shimmer progress.
    // Feature Request: Allow users to manipulate animationSpec, delay, and repeatMode for greater
    // customization.
    // TODO: The progress value appears to range from above 0 to below 1 in logs; this  behavior
    //  requires further investigation.
    // TODO: Migrate from `Modifier.composed` to `Modifier.Node` in the future.
    // TODO: Implement using `Animatable` API and allow customization of animationSpec, delay, and repeatMode.
    // TODO: Investigate how to support shimmer based on state change and single execution.
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Defines the animation for the shimmer, including duration, delay, and easing.
            animation = animationSpec,
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shimmer progress"
    )

    // Draws the shimmer effect on the content.
    drawWithContent {
        drawContent()
        val brushWidthPx = width.toPx()
        // Calculates the start and end positions of the shimmer gradient.
        // The shimmer effect transitions from the top-left to the bottom-right.
        // The brushWidthPx value represents the width of the shimmer, causing the gradient to extend
        // beyond the content's boundaries and create the illusion of movement.
        // Conceptual Enhancement: In the future, consider allowing the shimmer direction to be
        // customized (e.g., by specifying an angle)  to enable movement in various directions.
        // This could be visualized as the shimmer moving from one end to the other of a circle
        // that circumscribes the content rectangle plus the width of the brush.
        // TODO: Allow customization of shimmer direction (e.g., with something like angle) to
        //  support movement in different directions.
        // Calculates the start and end positions of the shimmer gradient.
        val (widthPx, heightPx) = size
        val startX = -brushWidthPx
        val startY = -brushWidthPx
        val endX = widthPx + brushWidthPx
        val endY = heightPx + brushWidthPx

        // Factor in the progress to get the current position
        val currentX = startX + (endX - startX) * progress
        val currentY = startY + (endY - startY) * progress


        // Draws a rectangle with the shimmer gradient.
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                // Use current position
                start = Offset(currentX, currentY),
                // Adjust end based on brush width
                end = Offset(currentX + brushWidthPx, currentY + brushWidthPx)
            ),
            blendMode = blendMode,
            size = size
        )
    }
}

fun Modifier.shimmer(
    color: Color = DefaultShimmerColor,
    width: Dp = DefaultShimmerWidth,
    blendMode: BlendMode = BlendMode.Hardlight,
    animationSpec: DurationBasedAnimationSpec<Float> = DefaultShimmerAnimationSpec
) = shimmer(listOf(Color.Transparent, color, Color.Transparent), width, blendMode, animationSpec)

