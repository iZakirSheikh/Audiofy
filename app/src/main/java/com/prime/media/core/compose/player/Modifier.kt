package com.prime.media.core.compose.player


import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize

private const val TAG = "Modifier"

private val ResizeMode.contentScale
    get() = when (this) {
        ResizeMode.Fit -> ContentScale.Fit
        ResizeMode.FixedWidth -> ContentScale.FillWidth
        ResizeMode.FixedHeight -> ContentScale.FillHeight
        ResizeMode.Fill -> ContentScale.FillBounds
        ResizeMode.Zoom -> ContentScale.Crop
    }

private fun Modifier.fixedWidth(
    aspectRatio: Float
) = clipToBounds()
    .fillMaxWidth()
    .wrapContentHeight(unbounded = true)
    .aspectRatio(aspectRatio)

private fun Modifier.fixedHeight(
    aspectRatio: Float
) = clipToBounds()
    .fillMaxHeight()
    .wrapContentWidth(unbounded = true)
    .aspectRatio(aspectRatio)

private fun Modifier.zoom(
    aspectRatio: Float
) = clipToBounds()
    .layout { measurable, constraints ->
        val maxWidth = constraints.maxWidth
        val maxHeight = constraints.maxHeight
        if (aspectRatio > maxWidth.toFloat() / maxHeight) {
            // wrap width unbounded
            val modifiedConstraints = constraints.copy(maxWidth = Constraints.Infinity)
            val placeable = measurable.measure(modifiedConstraints)
            layout(constraints.maxWidth, placeable.height) {
                val offsetX = Alignment.CenterHorizontally.align(
                    0, constraints.maxWidth - placeable.width, layoutDirection
                )
                placeable.place(IntOffset(offsetX, 0))
            }
        } else {
            // wrap height unbounded
            val modifiedConstraints = constraints.copy(maxHeight = Constraints.Infinity)
            val placeable = measurable.measure(modifiedConstraints)
            layout(placeable.width, constraints.maxHeight) {
                val offsetY =
                    Alignment.CenterVertically.align(0, constraints.maxHeight - placeable.height)
                placeable.place(IntOffset(0, offsetY))
            }
        }
    }
    .aspectRatio(aspectRatio)

/**
 * Returns a modifier that resizes the content according to the given aspect ratio and resize mode.
 * @param aspectRatio The desired aspect ratio of the content, such as 16f / 9f for 16:9 ratio.
 * @param resizeMode The resize mode that determines how the content is scaled and cropped to fit the container.
 * @return A modifier that applies the resize logic to the content.
 */
fun Modifier.resize(
    aspectRatio: Float, resizeMode: ResizeMode
) = when (resizeMode) {
    ResizeMode.Fit -> aspectRatio(aspectRatio)
    ResizeMode.Fill -> fillMaxSize()
    ResizeMode.FixedWidth -> fixedWidth(aspectRatio)
    ResizeMode.FixedHeight -> fixedHeight(aspectRatio)
    ResizeMode.Zoom -> zoom(aspectRatio)
}
