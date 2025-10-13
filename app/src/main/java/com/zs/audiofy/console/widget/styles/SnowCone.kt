@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zs.audiofy.console.widget.styles

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.console.RouteConsole
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.textResource
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Surface
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.Text
import com.zs.core.playback.NowPlaying

@Composable
fun SnowCone(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .fillMaxWidth()
            .widthIn(max = 120.dp)
            .visualEffect(ImageBrush.NoiseBrush, 0.4f, overlay = true)
            .border(AppTheme.colors.shine, RoundedCornerShape(16.dp))
            .background(AppTheme.colors.background(3.dp), RoundedCornerShape(16.dp))
            .shadow(8.dp, RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                textResource(R.string.widget_update_in_progress),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = ContentPadding.medium),
                style = AppTheme.typography.body2
            )
        }
    }
}