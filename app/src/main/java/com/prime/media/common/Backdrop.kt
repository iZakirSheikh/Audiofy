/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 17-11-2024.
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

@file:OptIn(ExperimentalHazeApi::class)

package com.prime.media.common

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.primex.core.ImageBrush
import com.primex.core.visualEffect
import com.zs.core_ui.Colors
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.hazeEffect

private const val TAG = "Backdrop"

/**
 * Creates and [remember] s the instance of [HazeState]
 */
@Composable
@NonRestartableComposable
fun rememberHazeState() = remember(::HazeState)

/**
 * Adds a subtle shine effect to components, particularly [Acrylic] ones,
 * mimicking the gleaming edge of glass.
 *
 * This property defines a [BorderStroke] that uses a vertical gradient.
 * The gradient's colors are determined by whether the current theme is light or dark.
 * - In a light theme, it transitions from the `background` color to a slightly darker,
 *   semi-transparent version of the `background`.
 * - In a dark theme, it transitions from a semi-transparent gray to an even more
 *   transparent gray.
 *
 * This creates a visual highlight, suggesting a light source reflecting off the edge
 * of the component.
 */
val Colors.shine
    @Composable @ReadOnlyComposable
    get() = BorderStroke(
        0.5.dp,
        Brush.verticalGradient(
            listOf(
                if (isLight) background else Color.Gray.copy(0.24f),
                if (isLight) background.copy(0.3f) else Color.Gray.copy(0.075f),
            )
        )
    )


/**
 * Constructs a regular style for light Theme.
 */
fun HazeStyle.Companion.Regular(
    containerColor: Color,
    tintAlpha: Float = if (containerColor.luminance() >= 0.5) 0.33f else 0.60f
) = HazeStyle(
    blurRadius = if (containerColor.luminance() >= 0.5f) 38.dp else 80.dp,
    backgroundColor = containerColor,
    noiseFactor = if (containerColor.luminance() >= 0.5f) 0.5f else 0.25f,
    tints =  listOf(
        HazeTint(Color.White.copy(0.05f), BlendMode.Luminosity),
        HazeTint(containerColor.copy(alpha = tintAlpha), blendMode = BlendMode.SrcOver),
    )
)

private val MistCachedPaint = Paint().apply {
    style = PaintingStyle.Stroke
    // Add blur effect on the Paint (but check if it renders well on lines)
    this.asFrameworkPaint().maskFilter =
        BlurMaskFilter(200f, BlurMaskFilter.Blur.NORMAL)
}

/**
 * Applies a mist effect to the Modifier by drawing multiple circles and rectangles with specified colors.
 *
 * @param containerColor The color to use for the main background.
 * @param accent The accent color to use for additional details.
 * @return A Modifier with the applied mist effect.
 */
private fun Modifier.mist(containerColor: Color, accent: Color) =
    drawBehind {
        // Determine if the container color is light based on its luminance.
        val isLight = containerColor.luminance() >= 0.5f

        // Draw the main rectangle with the container color.
        drawRect(containerColor)

        // Get the width and height of the drawing area and calculate the diameter for circles.
        val (w, h) = size
        val vertical = w < h
        val dp = if (vertical) h / 8 else w / 8
        val paint = MistCachedPaint

        // Set the paint color to accent and adjust the stroke width.
        paint.color = accent
        paint.strokeWidth = dp * 0.7f
        // Draw the first circle with the accent color.
        var x = if (!vertical) 2 * dp else size.center.x
        var y = if (!vertical) size.center.y else 2 * dp
        this.drawContext.canvas.drawCircle(
            Offset(x, y),
            dp,
            paint
        )

        // Change the paint color based on whether the container color is light or dark.
        paint.color = if (isLight) Color.Black else Color.White.copy(0.5f)
        // Draw the second circle with the modified color.
        x = if (!vertical) 3 * dp else size.center.x
        y = if (!vertical) size.center.y else 3 * dp
        this.drawContext.canvas.drawCircle(
            Offset(x, y),
            dp,
            paint
        )

        // Set the paint color to a slightly transparent version of the accent color.
        paint.color = accent.copy(0.7f)
        // Draw the third circle with the adjusted accent color.
        x = if (!vertical) 5 * dp else size.center.x
        y = if (!vertical) size.center.y else 5 * dp
        this.drawContext.canvas.drawCircle(
            Offset(x, y),
            dp,
            paint
        )

        // Reset the paint color to the original accent color.
        paint.color = accent

        // Draw the fourth circle with the accent color.
        x = if (!vertical) 8 * dp else size.center.x
        y = if (!vertical) size.center.y else 8 * dp
        this.drawContext.canvas.drawCircle(
            Offset(x, y),
            dp,
            paint
        )

        // Set the paint color to black.
        paint.color = Color.Black
        // Draw the fifth circle with black color.
        x = if (!vertical) 10 * dp else size.center.x
        y = if (!vertical) size.center.y else 10 * dp
        this.drawContext.canvas.drawCircle(
            Offset(x, y),
            dp,
            paint
        )

        // Draw the top rectangle with a slightly transparent version of the container color.
        drawRect(containerColor.copy(if (isLight) 0.73f else 0.60f))
    }
        // FIXME - Enabling this causes crash in android 9 on startup.
      //  .visualEffect(ImageBrush.NoiseBrush, 0.3f)

/**
 * Adds a dynamic backdrop to the Modifier based on the provided HazeState.
 *
 * @param state The current state of the haze effect, can be null.
 * @param style The style to apply to the haze effect.
 * @param fallbackContainerColor The color to use if the state is null.
 * @param fallbackAccent The accent color to use if the state is null.
 * @return A Modifier with the applied dynamic backdrop.
 */
fun Modifier.dynamicBackdrop(
    state: HazeState?,
    style: HazeStyle,
    fallbackContainerColor: Color,
    fallbackAccent: Color,
) = when (state) {
    null -> mist(fallbackContainerColor, fallbackAccent)
    else -> hazeEffect(state, style, HazeSettings)
}

val HazeSettings: (HazeEffectScope.() -> Unit) = { blurEnabled = true;
    // Adjust input scale for Android versions below 12 for better visuals.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        inputScale = HazeInputScale.Fixed(0.25f)
}