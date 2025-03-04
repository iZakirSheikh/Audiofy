/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 03-03-2025.
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

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.primex.core.MetroGreen
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val TAG = "ColorPicker"

// inspiration - https://github.com/mhssn95/compose-color-picker/blob/main/colorPicker/src/main/java/io/mhssn/colorpicker/data/Colors.kt

private val test = Color(0xFF1C0000)

/**Represents the color of [HueSlider]*/
private val HueSliderColors = listOf(
    Color(0xFFFF0000), // Red
    Color(0xffffff00), // Yellow
    Color(0xff00ff00), // Green
    Color(0xff00ffff), // Cyan
    Color(0xff0000ff), // Blue
    Color(0xffff00ff), // Magenta
    Color(0xffff0000)  // Red (to complete the hue circle)
)

/**
 * Calculates the color based on the given progress value.
 *
 * The progress value is a float ranging from 0.0 to 1.0, representing the position
 * within the hue spectrum. The function maps the progress value to one of the six
 * segments of the hue spectrum (Red to Yellow, Yellow to Green, Green to Cyan,
 * Cyan to Blue, Blue to Purple, and Purple to Red) and calculates the corresponding color.
 *
 * @param progress The progress value ranging from 0.0 to 1.0.
 * @return The calculated Color based on the progress value.
 */
private fun progressToColor(progress: Float): Color {
     // Calculate the hue value based on progress
    val hue = progress * 360

    // Convert the HSV color to RGB
    val color = Color.hsv(hue, 1f, 1f)

    return color
  /*  val scaledProgress = progress * 7
    val segment = scaledProgress.toInt()
    val segmentProgress = scaledProgress - segment

    val red: Int
    val green: Int
    val blue: Int

    // Determine the color range based on the progress value
    when (segment % 7) {
        0 -> {
            // Red to Yellow
            red = 255
            green = (255 * segmentProgress).roundToInt()
            blue = 0
        }

        1 -> {
            // Yellow to Green
            red = (255 * (1 - segmentProgress)).roundToInt()
            green = 255
            blue = 0
        }

        2 -> {
            // Green to Cyan
            red = 0
            green = 255
            blue = (255 * segmentProgress).roundToInt()
        }

        3 -> {
            // Cyan to Blue
            red = 0
            green = (255 * (1 - segmentProgress)).roundToInt()
            blue = 255
        }

        4 -> {
            // Blue to Magenta
            red = (255 * segmentProgress).roundToInt()
            green = 0
            blue = 255
        }

        5 -> {
            // Magenta to Red
            red = 255
            green = 0
            blue = (255 * (1 - segmentProgress)).roundToInt()
        }

        else -> {
            // This should never happen, but for safety:
            red = 255
            green = 0
            blue = 0
        }
    }
    // Return the calculated color
    return Color(red, green, blue)*/
}

/**
 * Converts a color to a progress value.
 *
 * This function takes a color and maps it to a progress value ranging from 0.0 to 1.0,
 * representing the position within the hue spectrum. The function calculates the hue component
 * of the color in the HSV (Hue, Saturation, Value) color model and normalizes it to a progress value.
 *
 * @param color The color to convert to a progress value.
 * @return The progress value ranging from 0.0 to 1.0 based on the hue of the color.
 */
private fun colorToProgress(color: Color): Float {
    // Extract the red, green, and blue components from the Color object
    val red = color.red
    val green = color.green
    val blue = color.blue
    // Calculate the maximum and minimum values among the RGB components
    val max = max(red, max(green, blue))
    val min = min(red, min(green, blue))

    // Calculate the difference (delta) between the max and min values
    val delta = max - min

    // Calculate the hue value based on the max RGB component and the delta
    val hue = when {
        delta == 0f -> 0f
        max == red -> (green - blue) / delta % 6
        max == green -> (blue - red) / delta + 2
        max == blue -> (red - green) / delta + 4
        else -> 0f
    }

    // Normalize the hue value to a progress value between 0.0 and 1.0
    return (hue / 6 + if (hue < 0) 1 else 0)
}

/**
 * Calculates the color at a given location within a color picker.
 *
 * This function takes a source color, the location of a touch event, the size of the color picker,
 * and an optional alpha value. It calculates the new color by applying lightening and darkening
 * factors based on the x and y positions of the touch event, respectively.
 *
 * @param source The source color.
 * @param location The location of the touch event.
 * @param size The size of the color palette board.
 * @param alpha The alpha value of the resulting color (default is 1.0f).
 * @return The calculated Color based on the provided parameters.
 */
private fun calculateColorAtLocation(
    source: Color,
    location: Offset,
    size: IntSize,
    alpha: Float = 1.0f
): Color {
    // Calculate the lighten factor based on the x position of the touch event
    // Calculate the darken factor based on the y position of the touch event
    // Convert the source color to ARGB format
    val lighten = 1 - (location.x / size.width)
    val darken = location.y / size.height
    val argb = source.toArgb()

    // Extract the red, green, and blue components from the ARGB value
    val r = argb shr 16 and 0xff
    val g = argb shr 8 and 0xff
    val b = argb and 0xff

    // Calculate the lightened values for red, green, and blue
    val lr = r + (255 - r) * lighten
    val lg = g + (255 - g) * lighten
    val lb = b + (255 - b) * lighten

    // Apply the darken factor to the lightened values
    val red = (lr - lr * darken).roundToInt()
    val green = (lg - lg * darken).roundToInt()
    val blue = (lb - lb * darken).roundToInt()

    // Return the new color with the calculated RGB values and the provided alpha value
    return Color(red, green, blue, (255 * alpha).roundToInt())
}

/**
 * @see calculateColorAtLocation
 */
private fun calculateLocationFromColor(source: Color): Offset {
    TODO("Not yet implemented!")
}

@Preview
@Composable
private fun Preview() {
    ColorPicker(
        Color.MetroGreen
    ) { }
}

private val DarkPalette = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
private val ColorPaletteHandleRadius = 8.dp


/**
 * Composable function that creates a color palette.
 *
 * This function displays a color palette that users can interact with to pick a color.
 * When a color is picked, the `onColorPicked` callback is invoked with the selected color.
 *
 * @param color The base color for the palette.
 * @param onColorPicked A callback function that is invoked when a color is picked.
 * @param modifier The modifier to be applied to the canvas.
 */
@Composable
private fun ColorPalette(
    color: Color,
    onColorPicked: (color: Color) -> Unit,
    modifier: Modifier = Modifier
) {
    // This represents the size of the color palette
    // This does not need to be state since we only use it in pointerInput.
    // The initial value is not important.
    var area = Size.Zero
    var location by remember { mutableStateOf(Offset.Infinite) }
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        // Clamp the x and y coordinates within the bounds of the area
                        val x = it.x.coerceIn(0f, area.width)
                        val y = it.y.coerceIn(0f, area.height)
                        // Update the location with the clamped coordinates
                        location = Offset(x, y)
                        // Calculate the new color based on the updated location
                        val newColor =
                            calculateColorAtLocation(color, location, area.toIntSize(), 1f)
                        // Invoke the callback to pass the new color
                        onColorPicked(newColor)
                    }
                }
                return@pointerInteropFilter true
            },
        onDraw = {
            // Set the area to the size of the canvas
            area = this.size
            // Construct the palette by drawing a horizontal gradient
            drawRect(Brush.horizontalGradient(listOf(Color.White, color)))
            drawRect(DarkPalette)

            // Draw the handle
            val radiusPx = ColorPaletteHandleRadius.toPx()
            // Set the initial handle location or use the updated location
            val newLocation = if (location == Offset.Infinite) Offset(
                size.width - radiusPx,
                0f + radiusPx
            ) else Offset(
                location.x.coerceIn(radiusPx, size.width - radiusPx),
                location.y.coerceIn(radiusPx, size.height - radiusPx)
            )
            // Define the stroke for the handle
            val stroke = Stroke(radiusPx * 0.45f)
            // Draw the handle circle with the calculated location
            drawCircle(Color.White, radius = radiusPx, center = newLocation, style = stroke)
        }
    )
}

private val HueSliderHeight = 22.dp
private val HueSliderShape = CircleShape
private val HueSliderBorder = BorderStroke(0.2.dp, Color.LightGray)

@Composable
private fun HueSlider(
    color: Color,
    onColorPicked: (color: Color) -> Unit,
    modifier: Modifier = Modifier
) {
    // This represents the size of the HueSlider
    // This does not need to be state since we only use it in pointerInputFilter.
    // The initial value is not important.
    // TODO - Use PointerInput and this is not required
    var area = Size.Zero
    var progress by remember { mutableFloatStateOf(colorToProgress(color)) }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(HueSliderHeight)
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        progress = (it.x / area.width).coerceIn(0f, 1f)
                        Log.d(TAG, "HueSlider: $progress")
                        val newColor = progressToColor(progress)
                        onColorPicked(newColor)
                    }
                }
                return@pointerInteropFilter true
            }
            .clip(HueSliderShape)
            .border(HueSliderBorder, HueSliderShape),
        onDraw = {
            area = size
            drawRect(
                Brush.horizontalGradient(
                    HueSliderColors,
                    startX = size.height / 2,
                    endX = size.width - size.height / 2
                )
            )
            val thumbRadiusPx = size.minDimension / 3.5f
            drawCircle(
                Color.White,
                radius = thumbRadiusPx,
                center = Offset(
                    thumbRadiusPx + (size.height / 2 - thumbRadiusPx) + ((size.width - (thumbRadiusPx + (size.height / 2 - thumbRadiusPx)) * 2) * progress),
                    size.height / 2
                ),
                style = Stroke(thumbRadiusPx * 0.45f)
            )
        }
    )
}

private val Spacing = Arrangement.spacedBy(8.dp)
private val Padding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)

@Composable
fun ColorPicker(
    source: Color,
    modifier: Modifier = Modifier,
    onColorPicked: (color: Color) -> Unit
) {
    val (color, onColorChange) = remember(source) { mutableStateOf(source) }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Spacing
    ) {
        ColorPalette(
            source,
            onColorPicked = onColorChange,
            modifier = Modifier
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Spacing,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .background(color, shape = CircleShape)
                    .border(1.dp, Color.LightGray, shape = CircleShape)
                    .size(56.dp)
            )

            HueSlider(
                source,
                onColorPicked = onColorPicked,
                modifier = Modifier.weight(1f)
            )
        }

        //TODO Remove this when new components are added
        Spacer(Modifier)
    }
}