package com.prime.media.common


import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

private const val TAG = "View"

/**
 * A wrapper around Media3 [PlayerView]
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun PlayerView(
    player: Player?,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(it).apply {
                hideController()
                useController = false
                this.player = player
                this.resizeMode = resizeMode
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                clipToOutline = true
                // Set the Background Color of the player as Solid Black Color.
                setBackgroundColor(Color.Black.toArgb())
                keepScreenOn = true
            }
        },
        update = { it.resizeMode = resizeMode; it.player = player;  it.keepScreenOn = true },
        onRelease = {it.player = null; it.keepScreenOn = false}
    )
}