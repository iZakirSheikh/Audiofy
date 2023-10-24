package com.prime.media.core.compose.player

import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize

private const val TAG = "Surface"


/**
 * Clears the video view from the player.
 * @param view The view that displays the video content, such as a SurfaceView or a TextureView.
 * @throws IllegalStateException If the view is not a valid surface view type.
 */
private fun Player.clearVideoView(view: View) {
    when (view) {
        is SurfaceView -> clearVideoSurfaceView(view)
        is TextureView -> clearVideoTextureView(view)
        else -> throw IllegalStateException()
    }
}

/**
 * Sets the video view for the player.
 * @param view The view that displays the video content, such as a SurfaceView or a TextureView.
 * @throws IllegalStateException If the view is not a valid surface view type.
 */
private fun Player.setVideoView(view: View) {
    when (view) {
        is SurfaceView -> setVideoSurfaceView(view)
        is TextureView -> setVideoTextureView(view)
        else -> throw IllegalStateException()
    }
}


/**
 * Returns the aspect ratio of the video size.
 * The aspect ratio is the ratio of the width to the height of the video.
 * @return The aspect ratio of the video size, or 0f if the height is zero.
 */
private val VideoSize.aspectRatio
    get() = if (height == 0) 0f else width * pixelWidthHeightRatio / height

/**
 * Returns the aspect ratio of the player's video content.
 * The aspect ratio is the ratio of the width to the height of the video.
 * @return The aspect ratio of the player's video content, or 0f if the player is null or has unknown video size.
 */
private val Player?.aspectRatio get() = (this?.videoSize ?: VideoSize.UNKNOWN).aspectRatio

/**
 * A composable function that displays a video surface for a given player.
 * @param player The player that provides the video content to display, or null if no player is available.
 * @param modifier The modifier to apply to the video surface, such as size, alignment, or padding.
 * @param surfaceType The type of surface view to use for the video surface, such as SurfaceView or TextureView.
 * @param resizeMode The resize mode to use for the video surface, such as Fit, Fill, FixedWidth, FixedHeight, or Zoom.
 */
// A composable function that displays a video surface for a given player.
@Composable
fun VideoSurface(
    player: Player?,
    modifier: Modifier = Modifier,
    surfaceType: SurfaceType = SurfaceType.SurfaceView,
    resizeMode: ResizeMode = ResizeMode.Fit
) {
    AndroidView(
        modifier = modifier
            .run {
                // Get the aspect ratio of the player's video content.
                val ration = player.aspectRatio

                // If the aspect ratio is zero or negative, fill the maximum size of the container.
                if (ration <= 0) fillMaxSize()

                // Otherwise, resize the content according to the aspect ratio and the resize mode.
                else resize(ration, resizeMode)
            }

            // Animate the size change of the content.
            .animateContentSize(),

        update = { view ->
            // If the view tag is not equal to the player, it means the player has changed.
            if (view.tag != player) {
                // Clear the previous player from the view.
                (view.tag as? Player)?.clearVideoView(view)
                // Set the view tag to the new player.
                view.tag = player
                // Set the new player to use the view as the video surface.
                player?.setVideoView(view)
            }
        },

        // A lambda function that updates the view when the player changes.
        factory = {
            // Create a view based on the surface type.
            val view = when (surfaceType) {
                // If the surface type is SurfaceView, create a SurfaceView instance.
                SurfaceType.SurfaceView -> SurfaceView(it)
                // Otherwise, create a TextureView instance.
                else -> TextureView(it)
            }

            // Set the view tag to the player.
            view.tag = player
            // Set the player to use the view as the video surface.
            player?.setVideoView(view)

            // Clip the view to its outline shape.
            view.clipToOutline = true

            // Set the layout parameters of the view.
            view.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Return the view as the result of the factory function.
            view
        },

        // A lambda function that clears the player from the view when it is released from composition.
        onRelease = { player?.clearVideoView(it) },
    )
}