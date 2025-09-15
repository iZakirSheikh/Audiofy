/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
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

package com.zs.audiofy.common.compose

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import com.zs.audiofy.R
import com.zs.audiofy.settings.AppConfig
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.background
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Colors
import com.zs.core.coil.ReBlurTransformation
import com.zs.core.coil.RsBlurTransformation
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient as Gradient

// Reusable mask
private val PROGRESSIVE_MASK = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
    Gradient(listOf(Color.Black, Color.Black, Color.Transparent))
else
    Gradient(0.5f to Color.Black, 0.8f to Color.Black.copy(0.5f), 1.0f to Color.Transparent)

/**
 * Applies a hazy effect to the background based on the provided [HazeState].
 *
 * This function creates a blurred background with optional noise and tint effects. It provides customization options
 * for blur radius, noise factor, tint color, blend mode, and progressive blurring.
 *
 * @param provider The [HazeState] instance that manages the haze effect.
 * @param containerColor The background color of the container. Defaults to [Colors.background].
 * @param blurRadius The radius of the blur effect. Defaults to 38.dp for light backgrounds (luminance >= 0.5) and 60.dp for dark backgrounds.
 * @param noiseFactor The factor for the noise effect. Defaults to 0.4f for light backgrounds and 0.28f for dark backgrounds. Noise effect is disabled on Android versions below 12.
 * @param tint The color to tint the blurred background with. Defaults to a semi-transparent version of [containerColor].
 * @param blendMode The blend mode to use for the tint. Defaults to [BlendMode.SrcOver].
 * @param luminance controls the luminosity of the blured layer defaults to 0.07f. -1 disables it.
 * @param progressive A float value to control progressive blurring:
 *   - -1f: Progressive blurring is disabled.
 *   - 0f: Bottom to top gradient.
 *   - 1f: Top to bottom gradient.
 *   - Values between 0f and 1f: Intermediate gradient positions.
 *   Progressive blurring is only available on Android 12 and above.
 * @return A [Background] composable with the specified haze effect.
 */
@SuppressLint("ModifierFactoryExtensionFunction")
@OptIn(ExperimentalHazeApi::class)
fun Colors.background(
    surface: HazeState,
    containerColor: Color = background(0.4.dp),
    blurRadius: Dp = if (containerColor.luminance() >= 0.5f) 38.dp else 80.dp,
    noiseFactor: Float = if (containerColor.luminance() >= 0.5f) 0.5f else 0.25f,
    tint: Color = containerColor.copy(alpha = if (containerColor.luminance() >= 0.5) 0.63f else 0.65f),
    luminance: Float = 0.07f,
    blendMode: BlendMode = BlendMode.SrcOver,
    progressive: Float = -1f,
) = if (!AppConfig.isBackgroundBlurEnabled) Background(Modifier.acrylic(background, accent))
else Background(Modifier.hazeEffect(state = surface) {
    this.blurEnabled = true
    this.blurRadius = blurRadius
    this.backgroundColor = containerColor
    // Disable noise factor on Android versions below 12.
    this.noiseFactor = noiseFactor
    this.tints = buildList {
        // apply luminosity just like in Microsoft Acrylic.
        if (luminance != -1f)
            this += HazeTint(Color.White.copy(0.07f), BlendMode.Luminosity)
        this += HazeTint(tint, blendMode = blendMode)
    }
    // Configure progressive blurring (if enabled).
    if (progressive != -1f) {
        this.progressive = HazeProgressive.verticalGradient(
            startIntensity = progressive,
            endIntensity = 0f,
            preferPerformance = true
        )
        // Adjust input scale for Android versions below 12 for better visuals.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            inputScale = HazeInputScale.Fixed(0.5f)
        mask = PROGRESSIVE_MASK
    }
})

/** Creates and [remember] s the instance of [HazeState] */
@Composable
@NonRestartableComposable
fun rememberAcrylicSurface() = remember(::HazeState)

/**
 * Marks the content to be a source of blurred content for the provided [surface].
 *
 * This function registers the content it is applied to as a source that contributes to the blurring
 * effect managed by the given [surface]. Any views or composables marked with [background] using
 * the same [surface] will blur this source content.
 *
 * @param surface The [HazeState] instance that this content will contribute to.
 * @return A [Modifier] that marks the content as a blur source.
 */
fun Modifier.source(surface: HazeState) = hazeSource(state = surface)

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
 * A Composable that displays an image with an acrylic blur effect.
 *
 * This Composable applies a blur effect to the provided image [data].
 * It uses a different blur implementation based on the Android SDK version:
 * - **Android S (API 31) and above:** Uses the built-in `Modifier.blur()` for a hardware-accelerated blur.
 * - **Below Android S:** Uses a [RsBlurTransformation] (RenderScript blur) for blurring the image.
 *
 * In both cases, a semi-transparent overlay ([containerColor]) is drawn on top,
 * and a subtle luminosity effect is applied using `BlendMode.DstOut` with a white color.
 * The alpha of this white color is adjusted based on whether the `containerColor` is light or dark.
 *
 * @param data The [Uri] of the image to display and blur. Can be null, in which case nothing is drawn.
 * @param modifier The [Modifier] to be applied to this Composable.
 */
@Composable
fun Acrylic(
    data: Uri?,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val trans = remember {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            RsBlurTransformation(ctx, radius = 25f, sampling = 3f)
        else
            ReBlurTransformation(25f, 3f)
    }
    val layer = rememberGraphicsLayer()
    val containerColor = AppTheme.colors.background

    Image(
        contentScale = ContentScale.Crop,
        contentDescription = null,
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(ctx)
                .data(data)
                .transformations(trans)
                .build()
        ),
        modifier = modifier
            // CompositingStrategy.Offscreen is often needed for custom drawing operations
            // to behave correctly, especially with transformations and blend modes.
            .drawWithCache {
                val texture = ShaderBrush(
                    ImageShader(
                        BitmapFactory.decodeResource(
                            ctx.resources,
                            R.drawable.noise,
                            BitmapFactory.Options().apply { inScaled = false }
                        ).asImageBitmap(),
                        TileMode.Repeated,
                        TileMode.Repeated
                    )
                )
                // CompositingStrategy.Offscreen is often needed for custom drawing operations
                // to behave correctly, especially with transformations and blend modes.
                layer.compositingStrategy = CompositingStrategy.Offscreen
                onDrawWithContent {
                    // Draw background
                    drawRect(color = containerColor)
                    // record content
                    layer.record {
                        val isLight = containerColor.luminance() > 0.5f
                        // Then, draw the (already blurred) image content.
                        this@onDrawWithContent.drawContent()

                        // Finally, apply the luminosity effect.
                        drawRect(
                            color = Color.White.copy(alpha = if (isLight) 0.83f else 0.94f),
                            // DstOut blend mode creates a cutout effect, enhancing luminosity.
                            blendMode = BlendMode.DstOut
                        ) // Alpha is adjusted based on light/dark theme.

                        drawRect(
                            brush = texture,
                            alpha = if (isLight) 0.1f else 0.05f,
                            blendMode = BlendMode.Hardlight
                        )
                    }
                    // draw layer
                    drawLayer(layer)
                }
            }
    )
}

private fun Modifier.acrylic(containerColor: Color, accent: Color) =
    background(containerColor)
        .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
        .drawWithCache {
            val paint = Paint().apply {
                style = PaintingStyle.Stroke
                // Add blur effect on the Paint (but check if it renders well on lines)
                this.asFrameworkPaint().maskFilter =
                    BlurMaskFilter(200f, BlurMaskFilter.Blur.NORMAL)
            }
            // Algorithm
            // This effect simulates the appearance of frosted glass or acrylic material. It involves:
            // 1. Drawing a base rectangle with the [containerColor].
            // 2. Drawing a series of blurred circles with varying colors and opacities, based on the [accent] color
            //    and the luminance of the [containerColor].
            // 3. Drawing an overlay rectangle with a slightly transparent version of the [containerColor].
            // 4. Applying a noise texture visual effect on top.
            // The positions and sizes of the circles are determined dynamically based on the dimensions
            // of the drawing area.
            onDrawBehind {
                // Determine if the container color is light based on its luminance.
                val isLight = containerColor.luminance() >= 0.5f

                // Draw the main rectangle with the container color.
                // drawRect(containerColor)

                // Get the width and height of the drawing area and calculate the diameter for circles.
                val (w, h) = size
                val vertical = w < h
                val dp = if (vertical) h / 8 else w / 8

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
                // drawRect(containerColor.copy(if (isLight) 0.73f else 0.60f))
                // Finally, apply the luminosity effect.
                drawRect(
                    color = Color.White.copy(alpha = if (isLight) 0.80f else 0.90f),
                    // DstOut blend mode creates a cutout effect, enhancing luminosity.
                    blendMode = BlendMode.DstOut
                ) // Alpha is adjusted based on light/dark theme.
            }
        }
        .visualEffect(ImageBrush.NoiseBrush, if (containerColor.luminance() > 0.5f) 0.1f else 0.05f)