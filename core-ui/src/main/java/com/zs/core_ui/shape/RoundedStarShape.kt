/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 26-02-2025.
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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Shape describing star with rounded corners
 *
 * Note: The shape draws within the minimum of provided width and height so can't be used to create stretched shape.
 *
 * @param sides number of sides.
 * @param curve a double value between 0.0 - 1.0 for modifying star curve.
 * @param rotation  value between 0 - 360
 * @param iterations a value between 0 - 360 that determines the quality of star shape.
 */
class RoundedStarShape(
    private val sides: Int,
    private val curve: Double = 0.09,
    private val rotation: Float = 0f,
    iterations: Int = 360,
) : Shape {

    private companion object {
        const val TWO_PI = 2 * PI
    }

    private val steps = (TWO_PI) / min(iterations, 360)
    private val rotationDegree = (PI / 180) * rotation

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Generic(Path().apply {


        val r = min(size.height, size.width) * 0.4 * mapRange(1.0, 0.0, 0.5, 1.0, curve)

        val xCenter = size.width * .5f
        val yCenter = size.height * .5f

        moveTo(xCenter, yCenter)

        var t = 0.0

        while (t <= TWO_PI) {
            val x = r * (cos(t - rotationDegree) * (1 + curve * cos(sides * t)))
            val y = r * (sin(t - rotationDegree) * (1 + curve * cos(sides * t)))
            lineTo((x + xCenter).toFloat(), (y + yCenter).toFloat())

            t += steps
        }

        val x = r * (cos(t - rotationDegree) * (1 + curve * cos(sides * t)))
        val y = r * (sin(t - rotationDegree) * (1 + curve * cos(sides * t)))
        lineTo((x + xCenter).toFloat(), (y + yCenter).toFloat())

    })


    private fun mapRange(a: Double, b: Double, c: Double, d: Double, x: Double): Double {
        return (x - a) / (b - a) * (d - c) + c
    }
}