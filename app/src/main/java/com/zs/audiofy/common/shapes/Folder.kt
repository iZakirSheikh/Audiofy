/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-05-2025.
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

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

private const val TAG = "Folder"

/**
 * Defines a [GenericShape] simpler to [Icons.Rounded.Folder]
 */
val FolderShape = GenericShape { (x, y), _ ->
    val radius = 0.18f * x
    val stepAt = 0.40f * x
    moveTo(radius, 0f)
    lineTo(stepAt, 0f)
    lineTo(stepAt + radius, radius)
    lineTo(x - radius, radius)
    quadraticTo(x, radius, x, 2 * radius)
    lineTo(x, y - radius)
    quadraticTo(x, y, x - radius, y)
    lineTo(radius, y)
    quadraticTo(0f, y, 0f, y - radius)
    lineTo(0f, radius)
    quadraticTo(0f, 0f, radius, 0f)
    close()
}

/**
 * A custom shape that resembles a folder with a curved top-right corner.
 *
 * @param radius The radius of the corners.
 */
class Folder(private val radius: Dp): Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // Create a Path object to define the shape
        val path = Path().apply {
            // Convert the radius from Dp to pixels
            val r = with(density){ radius.toPx() }
            // Get the width and height of the shape
            val (x, y ) = size
            // Calculate the x-coordinate for the step
            val stepAt = 0.40f * x
            // Define the path by moving to different points and drawing lines or curves
            moveTo(r, 0f)
            // Line to the step
            // then take the step; move to end
            lineTo(stepAt, 0f)
            lineTo(stepAt + r, r)
            lineTo(x - r, r)
            // Quadratic curve to the right edge
            quadraticTo(x, r, x, 2 * r)
            lineTo(x, y - r)
            quadraticTo(x, y, x - r, y)
            lineTo(r, y)
            quadraticTo(0f, y, 0f, y - r)
            lineTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            close()
            // Close the path
        }
        return Outline.Generic(path)
    }
}