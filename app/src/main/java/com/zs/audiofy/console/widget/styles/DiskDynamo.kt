package com.zs.audiofy.console.widget.styles

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.theme.Surface
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying

@Composable
fun DiskDynamo(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
){
    Surface(modifier.fillMaxWidth().height(360.dp)){
        Label("Not Implemented yet!")
    }
}