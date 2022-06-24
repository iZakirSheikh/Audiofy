package com.prime.player.extended.visualizers

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CircleLineVisualizer(
    modifier: Modifier = Modifier,
    density: Float,
    color: Color? = null,
    audioSessionID: Int
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CircleLineVisualizer(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setDensity(0.6f)
            }
        }
    ) {
        it.setAudioSessionId(audioSessionID)
        if (color != null) it.setColor(color.toArgb())
        it.setDensity(density)
    }
}


@Composable
fun BlobVisualizer(
    modifier: Modifier = Modifier,
    density: Float,
    color: Color? = null,
    audioSessionID: Int
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            com.gauravk.audiovisualizer.visualizer.BlobVisualizer(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setDensity(0.6f)
            }
        }
    ) {
        it.setAudioSessionId(audioSessionID)
        if (color != null) it.setColor(color.toArgb())
        it.setDensity(density)
    }
}