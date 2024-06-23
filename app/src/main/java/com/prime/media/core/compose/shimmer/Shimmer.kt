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

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus.Experimental

private const val TAG = "Shimmer"

private class ShimmerNode(
    var colors: List<Color>,
    var width: Dp,
    var blendMode: BlendMode,
    var spec: AnimationSpec<Float>
) : Modifier.Node(), DrawModifierNode {

    val animatable = Animatable(0f)

    // launch the quarantine at the time of the attach.
    override fun onAttach() {
        super.onAttach()
        coroutineScope.launch {
            animatable.animateTo(1f, spec)
        }
    }

    // Draw the effect.
    override fun ContentDrawScope.draw() {
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
        val progress = animatable.value
        val (widthPx, heightPx) = size
        val startX = -brushWidthPx
        val startY = -brushWidthPx
        val endX = widthPx + brushWidthPx
        val endY = heightPx + brushWidthPx

        // Factor in the progress to get the current position
        val currentX = startX + (endX - startX) * progress
        val currentY = startY + (endY - startY) * progress

        Log.d(TAG, "draw: $currentX, $currentY")
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

private class ShimmerNodeElement(
    var colors: List<Color>,
    var width: Dp,
    var blendMode: BlendMode,
    var spec: AnimationSpec<Float>
) : ModifierNodeElement<ShimmerNode>() {
    override fun create(): ShimmerNode = ShimmerNode(colors, width, blendMode, spec)

    override fun update(node: ShimmerNode) {
        node.colors = colors
        node.spec = spec
        node.width = width
        node.blendMode = blendMode
        Log.d(TAG, "update: colors: $colors, spec:$spec, width: $width, blendMode: $blendMode")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShimmerNodeElement

        if (colors != other.colors) return false
        if (width != other.width) return false
        if (blendMode != other.blendMode) return false
        if (spec != other.spec) return false

        return true
    }

    override fun hashCode(): Int {
        var result = colors.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + blendMode.hashCode()
        result = 31 * result + spec.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = TAG
        properties["colors"] = colors
        properties["width"] = width
        properties["bendsMode"] = blendMode
        properties["spec"] = spec
    }
}

private val DefaultShimmerColor = Color.Gray
private val DefaultShimmerColors = listOf(Color.Transparent, DefaultShimmerColor, Color.Transparent)
private val DefaultShimmerWidth = 30.dp
private val DefaultShimmerAnimationSpec = infiniteRepeatable(tween<Float>(1000, 2000))

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
    animationSpec: AnimationSpec<Float> = DefaultShimmerAnimationSpec
) = this then ShimmerNodeElement(colors, width, blendMode, animationSpec)

/**
 * @see shimmer
 */
fun Modifier.shimmer(
    color: Color = DefaultShimmerColor,
    width: Dp = DefaultShimmerWidth,
    blendMode: BlendMode = BlendMode.Hardlight,
    animationSpec: AnimationSpec<Float> = DefaultShimmerAnimationSpec
) = this then ShimmerNodeElement(
    listOf(Color.Transparent, color, Color.Transparent),
    width,
    blendMode,
    animationSpec
)