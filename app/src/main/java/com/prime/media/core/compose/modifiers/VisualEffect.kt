/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 24-01-2024.
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

package com.prime.media.core.compose.modifiers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import com.prime.media.R
import kotlin.system.measureNanoTime

private const val TAG = "VisualEffect"


/**
 * Represents an image used as a brush in Jetpack Compose [ShaderBrush] to create visual effects on composables,
 * such as textures, patterns, and gradients.
 *
 * This interface facilitates seamless integration of various visual effects, from subtle noise to bold brushstrokes,
 * empowering you to extend the visual aesthetics of your composables.
 *
 * It is optimized for performance, ensuring smooth and responsive rendering even with complex effects.
 *
 * Brush images must adhere to specific size(1.e., 128 x 128 px) and tiling requirements for optimal results.
 *
 * @see [ShaderBrush]
 */
sealed interface ImageBrush {

    companion object {

        /**
         * @see Resource
         */
        fun from(@DrawableRes value: Int): ImageBrush = Resource(value)

        /**
         * @see Image
         */
        fun from(value: Bitmap): ImageBrush = Image(value.asImageBitmap())


        /**
         * @see Image
         */
        fun from(value: ImageBitmap): ImageBrush = Image(value)

        /**
         * An [ImageBrush] that adds a subtle, organic noise texture to the composable.
         * Use BlendMode and alpha values to adjust the intensity and blending behavior for optimal results.
         */
        val NoiseBrush = from(R.drawable.noise)
    }
}


/**
 * Creates an [ImageBrush] from a drawable resource.
 *
 * @param id The resource ID of the drawable to use as the brush image.
 * @return An [ImageBrush] instance encapsulating the drawable resource.
 * @throws IllegalArgumentException if the provided drawable does not meet brush image requirements.
 */
@JvmInline
private value class Resource internal constructor(@DrawableRes val id: Int) : ImageBrush

/**
 * Creates an [ImageBrush] directly from an [ImageBitmap].
 *
 * @param bitmap The [ImageBitmap] to use as the brush image.
 * @return An [ImageBrush] instance encapsulating the provided [ImageBitmap].
 * @throws IllegalArgumentException if the provided [ImageBitmap] does not meet brush image requirements.
 */
@JvmInline
private value class Image internal constructor(val bitmap: ImageBitmap) : ImageBrush


/**
 * A private modifier node that applies a visual effect using an [ImageBrush] to a composable.
 *
 * This node supports customizing the effect's appearance through:
 *  - Alpha: Controls the effect's transparency.
 *  - Overlay: Determines whether the effect is drawn on top of or behind the content.
 *  - Blend mode: Specifies how the effect blends with the underlying content.
 *
 * It optimizes performance by caching the generated [ShaderBrush] to avoid unnecessary re-creation.
 *
 * @param brush The [ImageBrush] defining the visual effect.
 * @param alpha The opacity of the effect, ranging from 0.0 (fully transparent) to 1.0 (fully opaque).
 * @param overlay If true, the effect is drawn on top of the content; otherwise, it's drawn behind it.
 * @param blendMode The blend mode that controls how the effect interacts with the underlying content.
 *
 * @see Modifier.background
 */
private class EffectNode(
    var brush: ImageBrush,
    var alpha: Float,
    var overlay: Boolean,
    var blendMode: BlendMode
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    /**
     * Cached [ShaderBrush] generated from the [brush].
     */
    private var shaderBrush: ShaderBrush? = null

    /**
     * Last used [ImageBrush] for caching purposes.
     */
    private lateinit var lastBrush: ImageBrush

    /**
     * Creates a [ShaderBrush] from the provided [ImageBrush].
     *
     * @param value The [ImageBrush] to create the [ShaderBrush] from.
     * @return The constructed [ShaderBrush].
     */
    private fun create(value: ImageBrush): ShaderBrush {
        // Determine the appropriate image source based on the ImageBrush type
        val img = when (value) {
            // Directly use the ImageBitmap from the ImageBrush for direct ImageBitmap sources.
            is Image -> value.bitmap
            is Resource -> {
                // Decode the resource into a Bitmap for resource-based ImageBrushes.
                val resources = currentValueOf(LocalContext).resources
                val bmp: Bitmap
                val time = measureNanoTime {
                    // Decode the resource, disabling scaling for optimal performance.
                    bmp = BitmapFactory.decodeResource(
                        resources,
                        value.id,
                        BitmapFactory.Options().apply {
                            inScaled = false
//                          inTargetDensity = inDensity
//                          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                              this.inPreferredConfig = Bitmap.Config.HARDWARE
//                          }
                        }
                    )
                }
                Log.d(TAG, "create: (width=${bmp.width}, height=${bmp.height}, time=$time")
                // Convert the decoded Bitmap to an ImageBitmap for use in the ShaderBrush.
                bmp.asImageBitmap()
            }
        }
        // Create and return a ShaderBrush using the ImageShader for repeating the image:
        // TODO - Experiment with different tileModes.
        return ShaderBrush(ImageShader(img, TileMode.Repeated, TileMode.Repeated))
    }

    override fun ContentDrawScope.draw() {
        // Check if the effect should be entirely hidden due to zero alpha:
        if (alpha == 0f) {
            // Simply draw the content without applying any effect.
            drawContent()
            return
        }
        // Check if the cached ShaderBrush needs updated based on brush changes:
        // 1. If no ShaderBrush exists, or
        // 2. Last used brush doesn't match the current brush, or
        // 3. Last brush reference hasn't been initialized yet:
        val needsBrushUpdate =
            shaderBrush == null || lastBrush != brush || !::lastBrush.isInitialized
        if (needsBrushUpdate) {
            // Update the last brush reference and create a new ShaderBrush based on the current brush:
            lastBrush = brush
            shaderBrush = create(brush)
        }
        // Handle drawing order based on the overlay flag:
        if (overlay) {
            // Draw the content now if it's an overlay effect.
            drawContent()
        }

        // Draw the effect rectangle using the prepared ShaderBrush, alpha, and blend mode:
        drawRect(
            brush = shaderBrush!!,
            alpha = alpha,
            blendMode = blendMode,
        )

        if (!overlay) {
            // Draw the content first if it's not an overlay effect.
            drawContent()
        }
    }
}

/**
 * A private [ModifierNodeElement] that holds the properties for the visual effect and creates the corresponding [EffectNode].
 *
 * @param brush The [ImageBrush] for the effect.
 * @param alpha The opacity of the effect.
 * @param overlay Whether the effect is drawn as an overlay.
 * @param blendMode The blend mode for the effect.
 */
private class EffectElement(
    var brush: ImageBrush,
    var alpha: Float,
    var overlay: Boolean,
    var blendMode: BlendMode
) : ModifierNodeElement<EffectNode>() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EffectElement

        if (brush != other.brush) return false
        if (alpha != other.alpha) return false
        if (overlay != other.overlay) return false
        return blendMode == other.blendMode
    }

    override fun hashCode(): Int {
        var result = brush.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + overlay.hashCode()
        result = 31 * result + blendMode.hashCode()
        return result
    }

    /**
     * Creates the actual [EffectNode] to be used for drawing the effect.
     */
    override fun create() = EffectNode(brush, alpha, overlay, blendMode)

    /**
     * Updates the properties of an existing [EffectNode] to match this element's configuration.
     */
    override fun update(node: EffectNode) {
        node.blendMode = blendMode
        node.brush = brush
        node.overlay = overlay
        node.alpha = alpha
        Log.d(TAG, "update: (brush=$brush, alpha=$alpha, overlay=$overlay, blendMode=$blendMode)")
    }

    /**
     * Provides information for inspection tools to display the properties of this element.
     */
    override fun InspectorInfo.inspectableProperties() {
        name = "EffectElement"
        properties["alpha"] = alpha
        properties["BlendMode"] = blendMode
        properties["Brush"] = brush
        properties["overlay"] = overlay
    }
}

/**
 * Applies a visual effect using an [ImageBrush] to a composable.
 *
 * @param brush The [ImageBrush] defining the visual effect.
 * @param alpha The opacity of the effect, ranging from 0.0 (fully transparent) to 1.0 (fully opaque). Defaults to 1.0.
 * @param overlay If true, the effect is drawn on top of the content; otherwise, it's drawn behind it. Defaults to false.
 * @param blendMode The blend mode that controls how the effect interacts with the underlying content. Defaults to BlendMode.Overlay.
 *
 * @return A new modifier with the visual effect applied.
 */
fun Modifier.visualEffect(
    brush: ImageBrush,
    @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1.0f,
    overlay: Boolean = false,
    blendMode: BlendMode = BlendMode.Overlay
): Modifier = this then EffectElement(brush, alpha, overlay, blendMode)