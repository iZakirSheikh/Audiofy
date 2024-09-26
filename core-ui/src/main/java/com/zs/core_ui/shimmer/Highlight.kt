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

package com.zs.core_ui.shimmer

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val TAG = "Pulsate"

private class PulsateNode(
    var color: Color,
    var width: Dp,
    var animationSpec: AnimationSpec<Float>
) : Modifier.Node(), DrawModifierNode {

    // Animates the highlight effect using an infinite transition.
    val animation = Animatable(0f)

    override fun onAttach() {
        super.onAttach()
        coroutineScope.launch {
            animation.animateTo(
                1f,
                animationSpec
            )
        }
    }

    // Draws the highlight effect on the content.
    override fun ContentDrawScope.draw() {
        drawContent()
        val strokeWidthPx = width.toPx()
        val (widthPx, heightPx) = size
        val progress = animation.value
        // Calculates the radius of the highlight circle based on animation progress.
        val radius = ((kotlin.math.min(widthPx, heightPx) / 2) * progress)
        // Draws the highlight circle with adjusted alpha based on progress.
        // TODO: Enhance the visual effect by using a gradient instead of a solid color.
        //       The gradient should run from the middle of the stroke outwards.
        drawCircle(color.copy(1 - progress), style = Stroke(strokeWidthPx), radius = radius)
    }
}

private class PulsateNodeElement(
    var color: Color,
    var width: Dp,
    var animationSpec: AnimationSpec<Float>
) : ModifierNodeElement<PulsateNode>() {
    override fun create(): PulsateNode = PulsateNode(color, width, animationSpec)

    override fun update(node: PulsateNode) {
        node.color = color
        node.width = width
        node.animationSpec = animationSpec
        // TODO -  Call reset manually maybe.
        Log.d(TAG, "update: color: $color width: $width AnimationSpec: $animationSpec")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PulsateNodeElement

        if (color != other.color) return false
        if (width != other.width) return false
        if (animationSpec != other.animationSpec) return false

        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + animationSpec.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = TAG
        properties["color"] = color
        properties["width"] = width
        properties["animationSpec"] = animationSpec
    }
}

private val DEFAULT_PULSATE_ANIM_SPEC = infiniteRepeatable<Float>(tween(750, 2000))
private val DEFAULT_PULSATE_STROKE_WIDTH = 3.dp
private val DEFAULT_PULSATE_STROKE_COLOR = Color.White

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
 * Defaults to a [infiniteRepeatable] [tween] animation with a duration of 750ms and a delay of 1000ms.
 */
fun Modifier.pulsate(
    color: Color = DEFAULT_PULSATE_STROKE_COLOR,
    width: Dp = DEFAULT_PULSATE_STROKE_WIDTH,
    animationSpec: AnimationSpec<Float> = DEFAULT_PULSATE_ANIM_SPEC
) = this then PulsateNodeElement(color, width, animationSpec)