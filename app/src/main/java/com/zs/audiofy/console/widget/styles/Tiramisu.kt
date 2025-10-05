package com.zs.audiofy.console.widget.styles

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zs.compose.theme.Surface
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying

@Composable
fun Tiramisu(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier.height(60.dp)){
        Label("Not Implemented yet!")
    }
}