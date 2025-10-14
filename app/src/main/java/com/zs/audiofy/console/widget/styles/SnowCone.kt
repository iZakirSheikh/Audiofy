/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 28-08-2024.
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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zs.audiofy.console.widget.styles


import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying

private val SnowConeShape = RoundedCornerShape(14)
private val DefaultArtworkShape = RoundedCornerShape(20)
private val DefaultArtworkSize = 84.dp

/**
 * A mini-player inspired by android 12 notification
 */
@Composable
fun SnowCone(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    BaseListItem(
        contentColor = colors.onBackground,
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .heightIn(max = 120.dp)
            .shadow(16.dp, SnowConeShape)
            .border(0.5.dp, colors.background(20.dp), SnowConeShape)
            .background(AppTheme.colors.background(1.dp)),
        // subtitle
        heading = {
            Label(
                state.subtitle ?: textResource(R.string.unknown),
                style = AppTheme.typography.label3,
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                modifier = Modifier.sharedElement(RouteConsole.ID_SUBTITLE),
            )
        },
        // title
        overline = {
            Box(
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_TITLE)
                    .clipToBounds(),
                content = {
                    Label(
                        state.title ?: stringResource(R.string.unknown),
                        style = AppTheme.typography.title2,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.marque(Int.MAX_VALUE)
                    )
                }
            )
        },
        // AlbumArt
        leading = {
            AsyncImage(
                model = state.artwork,
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .clip(AppTheme.shapes.medium)
                    .background(AppTheme.colors.background(1.dp))
                    .size(DefaultArtworkSize),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        },
        // control centre
        trailing = {
            Column {
                // Expand to fill
                IconButton(
                    icon = Icons.Outlined.Tune,
                    contentDescription = null,
                    onClick = { onRequest(Widget.REQUEST_SHOW_CONFIG) },
                    modifier = Modifier.offset(10.dp, -10.dp)
                )

                // Open Console
                IconButton(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                    modifier = Modifier.sharedElement(RouteConsole.ID_BTN_COLLAPSE) then Modifier.offset(
                        10.dp,
                        -4.dp
                    )
                )
            }
        },
        // play controls
        subheading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    val color = AppTheme.colors.onBackground
                    // SeekBackward
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        tint = color
                    )

                    // Play/Pause
                    LottieAnimatedButton(
                        id = R.raw.lt_play_pause2,
                        atEnd = state.playing,
                        scale = 1.5f,
                        progressRange = 0.1f..0.65f,
                        animationSpec = tween(easing = LinearEasing),
                        onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_PLAY_PAUSE)
                    )

                    // SeekNext
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        tint = color
                    )
                }
            )
        }
    )
}