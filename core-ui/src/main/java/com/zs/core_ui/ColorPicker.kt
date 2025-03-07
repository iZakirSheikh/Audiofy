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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.window.DialogProperties
import com.primex.core.Amber
import com.primex.core.AzureBlue
import com.primex.core.BlackOlive
import com.primex.core.CapriBlue
import com.primex.core.ClaretViolet
import com.primex.core.DahliaYellow
import com.primex.core.Ivory
import com.primex.core.JetBlack
import com.primex.core.LightBlue
import com.primex.core.MetroGreen
import com.primex.core.MetroGreen2
import com.primex.core.OliveYellow
import com.primex.core.Orange
import com.primex.core.OrientRed
import com.primex.core.RedViolet
import com.primex.core.Rose
import com.primex.core.SepiaBrown
import com.primex.core.SkyBlue
import com.primex.core.TrafficBlack
import com.primex.core.TrafficYellow
import com.primex.core.UmbraGrey
import com.primex.core.thenIf
import com.primex.material2.Dialog
import com.primex.material2.IconButton
import com.primex.material2.Label
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

private const val TAG = "ColorPicker"

// See Also - https://github.com/mhssn95/compose-color-picker/blob/main/colorPicker/src/main/java/io/mhssn/colorpicker/data/Colors.kt

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
 * Converts a progress value (between 0.0 and 1.0) to a corresponding color in the HSV color space.
 * The progress determines the hue of the color, while saturation and value are kept at their maximum (1.0).
 *
 * @param progress A float value representing the progress, typically between 0.0 and 1.0.
 *                 Values outside this range will be handled, but the resulting hue will wrap around.
 *                 e.g. -0.5 will be the same hue as 0.5, and 1.5 will be the same as 0.5
 * @return A Color object representing the color corresponding to the given progress. The color will have a full saturation and value.
 *         The hue will smoothly transition from 0 to 360 degrees as progress goes from 0.0 to 1.0, creating a rainbow spectrum.
 */
private fun progressToColor(progress: Float): Color {
    // Calculate the hue value based on progress
    val hue = progress * 360
    // Convert the HSV color to RGB
    val color = Color.hsv(hue, 1f, 1f)
    return color
}

/**
 * Converts a Compose Color to a progress value representing its hue.
 *
 * This function takes a [Color] object and extracts its hue component,
 * returning a normalized progress value between 0.0 and 1.0.
 *
 * @param color The [Color] object to convert.
 * @return A [Float] representing the hue as a progress value (0.0 to 1.0).
 *         Returns 0.0 if the color is invalid.
 */
private fun colorToProgress(color: Color): Float {
    // Convert Color to ARGB
    val argb = color.toArgb()

    // Extract the Hue using Android's ColorUtils
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(argb, hsv)

    // Return the hue as a progress value (0.0 to 1.0)
    return hsv[0] / 360f
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

/**Represents the brightness of the ColorPallet*/
private val Brightness = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
private val ColorPaletteHandleRadius = 8.dp

/**
 * Composable function that creates a color palette.
 *
 * This function displays a color palette that users can interact with to pick a color.
 * When a color is picked, the `onColorPicked` callback is invoked with the selected color.
 *
 * @param hue The base color for the palette.
 * @param onColorPicked A callback function that is invoked when a color is picked.
 * @param modifier The modifier to be applied to the canvas.
 */
@Composable
private fun ColorPalette(
    hue: Color,
    onColorPicked: (color: Color) -> Unit,
    modifier: Modifier = Modifier
) {
// Represents the size of the color palette area (initially set to zero)
    // This is not stateful because it's only used within pointer input handling.
    var area = Size.Zero

    // Location of the user's current selection on the color board.
    // When the seed color changes, reset the selection to an initial invalid location (Offset.Infinite).
    var location by remember(hue) {
        onColorPicked(hue)
        mutableStateOf(Offset.Infinite)
    }

    // Canvas used to display the color palette and allow color selection via touch input.
    // This palette is used to select the seed color and adjust the brightness.
    // The horizontal axis (width) represents the lightness of the color,
    // while the vertical axis represents the brightness.
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
                            calculateColorAtLocation(hue, location, area.toIntSize(), 1f)
                        // Invoke the callback to pass the new color
                        onColorPicked(newColor)
                    }
                }
                return@pointerInteropFilter true
            },
        onDraw = {
            // Set the area to the size of the canvas
            area = this.size
            // Create a horizontal gradient from white to the seed color, representing lightness.
            drawRect(Brush.horizontalGradient(listOf(Color.White, hue))) // Lightness gradient.
            // Add a placeholder or actual drawing for brightness (if needed, you can define a gradient for brightness).
            drawRect(Brightness) // This can be customized to visualize the brightness.
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


/**
 * A Composable function that renders a horizontal hue slider where users can select a color hue.
 * The slider displays a gradient representing the full spectrum of hues, and users can adjust
 * the hue by dragging the thumb along the slider.
 *
 * The selected hue value is updated based on the user's interaction, and the corresponding color
 * is passed back through the [onHuePicked] callback.
 *
 * @param hue The current hue color, which is used to initialize the slider's progress.
 * @param onHuePicked A callback function that is triggered when the user selects a new hue color.
 * This function receives the new color as a parameter.
 * @param modifier A modifier to customize the appearance and layout of the slider. Default is [Modifier].
 */
@Composable
private fun HueSlider(
    hue: Color,
    onHuePicked: (color: Color) -> Unit,
    modifier: Modifier = Modifier
) {
    // Represents the size of the HueSlider area. This is used to calculate where the user is touching.
    // The initial value is not important because we update it dynamically in the pointer input handler.
    // TODO: Use PointerInput for more modern and flexible handling of touch events (pointerInteropFilter can be replaced).
    var area = Size.Zero

    // State to store the current progress of the hue slider (range: 0.0 to 1.0).
    // The progress is initialized based on the initial hue value.
    var progress by remember(hue) { mutableFloatStateOf(colorToProgress(hue)) }

    // Draw the slider
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(HueSliderHeight)
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        // Calculate the progress based on the user's horizontal touch position.
                        // Clamp the progress between 0 and 1 based on the width of the slider.
                        progress = (it.x / area.width).coerceIn(0f, 1f)
                        Log.d(TAG, "HueSlider: $progress")
                        // Calculate the new color based on the progress and trigger the callback.
                        val newColor = progressToColor(progress)
                        onHuePicked(newColor)
                    }
                }
                return@pointerInteropFilter true
            }
            .clip(HueSliderShape)
            .border(HueSliderBorder, HueSliderShape),
        onDraw = {
            // Update the area size to match the size of the canvas.
            area = size
            // Draw a horizontal gradient to represent the hue spectrum (rainbow).
            drawRect(
                Brush.horizontalGradient(
                    HueSliderColors,
                    startX = size.height / 2,
                    endX = size.width - size.height / 2
                )
            )

            // Calculate the radius of the thumb (the handle the user interacts with) based on the slider's height.
            val thumbRadiusPx = size.minDimension / 3.5f

            // Draw the thumb (circle) that the user moves along the slider.
            // The position of the thumb is based on the calculated progress.
            drawCircle(
                Color.White,
                radius = thumbRadiusPx,
                // Calculate the X-coordinate of the thumb's center based on progress.
                // The Y-coordinate of the thumb is always centered vertically.
                center = Offset(
                    thumbRadiusPx + (size.height / 2 - thumbRadiusPx) + ((size.width - (thumbRadiusPx + (size.height / 2 - thumbRadiusPx)) * 2) * progress),
                    size.height / 2
                ),
                style = Stroke(thumbRadiusPx * 0.45f)
            )
        }
    )
}

private val DialogProperties = DialogProperties(dismissOnClickOutside = false)

private val ColorSwatch = arrayOf(
    Color.CapriBlue,
    Color.SkyBlue,
    Color.LightBlue,
    Color.Orange,
    Color.Rose,
    Color.OrientRed,
    Color.RedViolet,
    Color.ClaretViolet,
    Color.Magenta,
    Color.AzureBlue,
    Color.MetroGreen,
    Color.MetroGreen2,
    Color.OliveYellow,
    Color.Ivory,
    Color.TrafficYellow,
    Color.DahliaYellow,
    Color.Amber,
    Color.BlackOlive,
    Color.SepiaBrown,
    Color.UmbraGrey,
    Color.JetBlack,
    Color.TrafficBlack,
)

@Composable
private fun ColorSwatch(
    hue: Color,
    onHuePicked: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (color in ColorSwatch) {
            Spacer(
                modifier = Modifier
                    .thenIf(color == hue) { scale(1.15f) }
                    .size(width = 15.dp, height = 45.dp)
                    .weight(1f)
                    .background(color)
                    .clickable() { onHuePicked(color) }
            )
        }
    }
}

/*The space between the component of screen*/
private val ItemSpacing = Arrangement.spacedBy(8.dp)
private val hItemPadding = PaddingValues(horizontal = 12.dp)

@Composable
fun ColorPickerDialog(
    expanded: Boolean,
    initial: Color,
    onColorPicked: (Color) -> Unit
) {
    Dialog(
        expanded,
        onDismissRequest = { onColorPicked(Color.Unspecified) },
        properties = DialogProperties,
        content = {
            Column {
                TopAppBar(
                    title = { Label("Color Picker", style = AppTheme.typography.titleSmall) },
                    navigationIcon = {
                        IconButton(
                            Icons.Default.Close,
                            onClick = {
                                onColorPicked(Color.Unspecified)
                            }
                        )
                    },
                    backgroundColor = AppTheme.colors.background(2.dp),
                    contentColor = LocalContentColor.current,
                    elevation = 0.dp
                )

                // content
                val (hue, onHueChange) = remember { mutableStateOf(initial) }
                val (color, onBrightnessChange) =
                    remember(hue) { mutableStateOf(initial) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = ItemSpacing
                ) {

                    ColorPalette(hue, onBrightnessChange)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = ItemSpacing,
                        modifier = Modifier.padding(hItemPadding),
                        content = {

                            Spacer(
                                modifier = Modifier
                                    .drawBehind {
                                        drawCircle(color = color)
                                        drawCircle(color = Color.LightGray, style = Stroke(1.dp.toPx()))
                                    }
                                    .size(56.dp)
                            )

                            HueSlider(
                                hue,
                                onHuePicked = onHueChange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    )

                    Label(
                        "Swatches",
                        style = AppTheme.typography.caption,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .padding(horizontal = 12.dp)
                            .align(Alignment.Start)
                    )

                    ColorSwatch(hue, onHueChange, modifier = Modifier.padding(hItemPadding))

                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = {
                        TextButton(onClick = { onColorPicked(Color.Unspecified) }) {
                            Label(text = stringResource(id = android.R.string.cancel))
                        }
                        TextButton(onClick = { onColorPicked(color) }) {
                            Label(text = stringResource(id = android.R.string.ok))
                        }
                    }
                )
            }
        }
    )
}