/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 27-02-2025.
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

package com.zs.core_ui.shape

import androidx.annotation.FloatRange
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A custom [Shape] that creates a rectangle with rounded corners and a skewed left side
 *
 *
 * The skew is applied as a fraction of the total height, creating a parallelogram-like effect.
 * The top and bottom edges are shifted horizontally by `skew * height / 2`.
 *
 * @property cornerRadius The radius of the rounded corners.
 * @property skew The skew factor, ranging from 0.0 (no skew) to 1.0 (maximum skew).
 * @constructor Creates a [SkewedRoundedRectangleShape] with the given corner radius and skew.
 *
 * Example :
 * ```
 *  Box(
 *      modifier = Modifier
 *          .fillMaxWidth()
 *          .height(100.dp)
 *          .clip(SkewedRoundedRectangleShape(cornerRadius = 16.dp, skew = 0.3f))
 *          .background(Color.Blue)
 *  )
 * ```
 */
class SkewedRoundedRectangleShape(
    private val cornerRadius: Dp,
    @FloatRange(from = 0.0, to = 1.0) private val skew: Float = 0.2f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Generic(
        Path().apply {
            // Get the width and height of the shape
            val (width, height) = size
            // Convert the cornerRadius to pixels using the density
            val cornerRadiusPx = with(density) { cornerRadius.toPx() }

            // Calculate the short side height and its half
            val shortBy = height * skew
            val halfShortBy = shortBy / 2

            // Move to the initial position (top-left corner) with the corner radius
            moveTo(cornerRadiusPx, 0f)
            // Draw a line from the top-left corner to the top-right corner with an offset
            lineTo(width - cornerRadiusPx, halfShortBy)

            // Draw the top-end curve using quadratic Bézier curve
            quadraticTo(
                width, halfShortBy,
                width, cornerRadiusPx + halfShortBy
            )

            // Draw a line from the top-right corner to the bottom-right corner with an offset
            lineTo(width, height - halfShortBy - cornerRadiusPx)

            // Draw the bottom-end curve using quadratic Bézier curve
            quadraticTo(
                width, height - halfShortBy,
                width - cornerRadiusPx, height - halfShortBy
            )

            // Draw a line from the bottom-right corner to the bottom-left corner
            lineTo(cornerRadiusPx, height)

            // Draw the bottom-start curve using quadratic Bézier curve
            quadraticTo(
                0f, height,
                0f, height - cornerRadiusPx
            )

            // Draw a line from the bottom-left corner to the top-left corner
            lineTo(0f, cornerRadiusPx)

            // Draw the top-start curve using quadratic Bézier curve
            quadraticTo(
                0f, 0f,
                cornerRadiusPx, 0f
            )

            // Close the path
            close()
        }
    )
}
