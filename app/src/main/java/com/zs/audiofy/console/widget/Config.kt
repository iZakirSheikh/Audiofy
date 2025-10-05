package com.zs.audiofy.console.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zs.core.playback.NowPlaying
import dev.chrisbanes.haze.HazeState

/**
 * Represents the widget preview flyout
 */
context(_: Widget)
@Composable
fun Config(
    state: NowPlaying,
    surface: HazeState,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {

}