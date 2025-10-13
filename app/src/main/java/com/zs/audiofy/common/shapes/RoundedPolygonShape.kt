/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 20-09-2024.
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

package com.zs.audiofy.common.shapes

import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.max

private fun RoundedPolygon.getBounds() = calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) }

/**
 * A shape representing a rounded polygon.
 *
 * This shape creates a polygon with the specified number of sides and applies rounding to its corners.
 * The rounding is controlled by the `rounding` parameter, which represents the fraction of the side length used for rounding.
 *
 * @param sides The number of sides of the polygon. Must be 3 or greater.
 * @param rounding The rounding factor, as a fraction of the side length. Should be a value between 0.0 (no rounding) and 1.0 (maximum rounding).
 */
class RoundedPolygonShape(
    @IntRange(3) private val sides: Int,
    @FloatRange(0.0, 1.0) private val rounding: Float = 0.0f
): Shape {

    private val polygon = RoundedPolygon(sides, rounding = CornerRounding(rounding, 1f))
    private var path = Path()
    private var matrix = Matrix()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        path.rewind()
        path = polygon.toPath().asComposePath()
        matrix.reset()
        val bounds = polygon.getBounds()
        val maxDimension = max(bounds.width, bounds.height)
        matrix.scale(size.width / maxDimension, size.height / maxDimension)
        matrix.translate(-bounds.left, -bounds.top)

        path.transform(matrix)
        return Outline.Generic(path)
    }
}