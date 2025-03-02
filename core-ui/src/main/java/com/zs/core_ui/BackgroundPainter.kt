/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 25-02-2025.
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

package com.zs.core_ui

import android.annotation.SuppressLint
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastRoundToInt

/**
 * A modifier that paints background with [painter].
 */
fun Modifier.background(
    painter: Painter,
    alignment: Alignment = Alignment.Center,
    overlay: Color = Color.Unspecified,
    contentScale: ContentScale = ContentScale.Inside,
) =  this then
        BackgroundPainterElement(
            painter = painter,
            alignment = alignment,
            contentScale = contentScale,
            overlay = overlay
        )

private data class BackgroundPainterElement(
    val painter: Painter,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val overlay: Color,
) : ModifierNodeElement<BackgroundPainterNode>() {
    override fun create(): BackgroundPainterNode {
        return BackgroundPainterNode(
            painter = painter,
            alignment = alignment,
            contentScale = contentScale,
            overlay = overlay
        )
    }

    override fun update(node: BackgroundPainterNode) {
        node.painter = painter
        node.alignment = alignment
        node.contentScale = contentScale
        node.overlay = overlay
        // redraw because one of the node properties has changed.
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "paint"
        properties["painter"] = painter
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["overlay"] = overlay
    }
}

/**
 * [DrawModifier] used to draw the provided [Painter] followed by the contents of the component
 * itself
 *
 * IMPORTANT NOTE: This class sets [androidx.compose.ui.Modifier.Node.shouldAutoInvalidate] to false
 * which means it MUST invalidate both draw and the layout. It invalidates both in the
 * [PainterElement.update] method through [LayoutModifierNode.invalidateLayer] (invalidates draw)
 * and [LayoutModifierNode.invalidateLayout] (invalidates layout).
 */
private class BackgroundPainterNode(
    var painter: Painter,
    var alignment: Alignment,
    var contentScale: ContentScale,
    var overlay: Color,
): Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    override fun ContentDrawScope.draw() {
        val srcSize = size

        // Compute the offset to translate the content based on the given alignment
        // and size to draw based on the ContentScale parameter
        val scaledSize =
            if (size.width != 0f && size.height != 0f) {
                srcSize * contentScale.computeScaleFactor(srcSize, size)
            } else {
                Size.Zero
            }

        val alignedPosition =
            alignment.align(
                IntSize(scaledSize.width.fastRoundToInt(), scaledSize.height.fastRoundToInt()),
                IntSize(size.width.fastRoundToInt(), size.height.fastRoundToInt()),
                layoutDirection
            )

        val dx = alignedPosition.x.toFloat()
        val dy = alignedPosition.y.toFloat()

        // Only translate the current drawing position while delegating the Painter to draw
        // with scaled size.
        // Individual Painter implementations should be responsible for scaling their drawing
        // content accordingly to fit within the drawing area.
        translate(dx, dy) {
            with(painter) { draw(size = scaledSize) }
        }

        if (overlay.isSpecified)
            drawRect(overlay)

        // Maintain the same pattern as Modifier.drawBehind to allow chaining of DrawModifiers
        this@draw.drawContent()
    }
}