/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.console.widget

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.zs.audiofy.common.compose.VideoSurface
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.resize
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.console.RouteConsole
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.foreground
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LinearProgressIndicator
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.VideoProvider

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FabVideoPlayer(
    state: NowPlaying,
    videoProvider: VideoProvider,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = AppTheme.shapes.medium
    Box(
        modifier = modifier
            .sharedElement(RouteConsole.ID_BACKGROUND)
            .border(AppTheme.colors.shine, shape)
            .shadow(8.dp, shape)
            .background(AppTheme.colors.background(1.dp)) then Widget.FabVideoSize,
        content = {
            // Video (includes foreground 35%)
            VideoSurface(
                videoProvider,
                keepScreenOn = true,
                modifier = Modifier
                    .sharedBounds(RouteConsole.ID_VIDEO_SURFACE)
                    .resize(ContentScale.Crop, state.videoSize)
                    .clip(shape)
                    .foreground(Color.Black.copy(0.25f))
            )
            // controls
            val chronometer = state.chronometer
            LinearProgressIndicator(
                progress = chronometer.progress(state.duration),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(3.dp)
            )

            // Close
            IconButton(
                Icons.Outlined.Close,
                onClick = { onRequest(Widget.REQUEST_CLOSE) },
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd),
                tint = Color.SignalWhite
            )

            // Expand
            IconButton(
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopStart),
                tint = Color.SignalWhite
            )

            if (!state.playing)
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).minimumInteractiveComponentSize(),
                    tint = Color.SignalWhite
                )
        }
    )
}