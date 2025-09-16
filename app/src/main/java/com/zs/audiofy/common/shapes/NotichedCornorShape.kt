/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 18-10-2024.
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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection as LD

/**
 * Represents a [Shape] with corners notched by [radius]
 *
 * @param radius size of the notches.
 */
fun NotchedCornerShape(radius: Dp) = object:  Shape {
    override fun createOutline(size: Size, layoutDirection: LD, density: Density): Outline {
        val path = Path().apply {
            reset()
            val cornerRadius = with(density) { radius.toPx() }
            // Top left arc
            arcTo(
                rect = Rect(
                    left = -cornerRadius,
                    top = -cornerRadius,
                    right = cornerRadius,
                    bottom = cornerRadius
                ),
                startAngleDegrees = 90.0f,
                sweepAngleDegrees = -90.0f,
                forceMoveTo = false
            )
            lineTo(x = size.width - cornerRadius, y = 0f)
            // Top right arc
            arcTo(
                rect = Rect(
                    left = size.width - cornerRadius,
                    top = -cornerRadius,
                    right = size.width + cornerRadius,
                    bottom = cornerRadius
                ),
                startAngleDegrees = 180.0f,
                sweepAngleDegrees = -90.0f,
                forceMoveTo = false
            )
            lineTo(x = size.width, y = size.height - cornerRadius)
            // Bottom right arc
            arcTo(
                rect = Rect(
                    left = size.width - cornerRadius,
                    top = size.height - cornerRadius,
                    right = size.width + cornerRadius,
                    bottom = size.height + cornerRadius
                ),
                startAngleDegrees = 270.0f,
                sweepAngleDegrees = -90.0f,
                forceMoveTo = false
            )
            lineTo(x = cornerRadius, y = size.height)
            // Bottom left arc
            arcTo(
                rect = Rect(
                    left = -cornerRadius,
                    top = size.height - cornerRadius,
                    right = cornerRadius,
                    bottom = size.height + cornerRadius
                ),
                startAngleDegrees = 0.0f,
                sweepAngleDegrees = -90.0f,
                forceMoveTo = false
            )
            lineTo(x = 0f, y = cornerRadius)
            close()
        }
        return Outline.Generic(path)
    }
}

