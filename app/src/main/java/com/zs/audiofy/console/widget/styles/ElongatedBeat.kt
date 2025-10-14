/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 26-02-2025.
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.FloatingActionButton
import com.zs.compose.theme.FloatingActionButtonDefaults
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.audiofy.common.compose.ContentPadding as CP

private val DefaultArtworkSize = DpSize(84.dp, 1.5 * 84.dp)
private val DefaultArtworkShape = CircleShape
private val Shape = RoundedCornerShape(14)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ElongatedBeat(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = AppTheme.colors.accent
    BaseListItem(
        contentColor = AppTheme.colors.onAccent,
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .shadow(8.dp, Shape)
            .border(1.dp, Color.Gray.copy(0.24f), Shape)
            .background(accent),
        // Playing bars.
        overline = {
            Icon(
                painter = lottieAnimationPainter(
                    R.raw.playback_indicator,
                    isPlaying = state.playing
                ),
                contentDescription = null,
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_PLAYING_INDICATOR)
                    .lottie(),
            )
        },
        // title + subtitle
        heading = {
            Column {
                Box(
                    modifier = Modifier
                        .sharedElement(RouteConsole.ID_TITLE)
                        .clipToBounds(),
                    content = {
                        Label(
                            state.title ?: stringResource(R.string.unknown),
                            modifier = Modifier.marque(Int.MAX_VALUE),
                            style = AppTheme.typography.title1,
                        )
                    }
                )
                Label(
                    state.subtitle ?: stringResource(R.string.unknown),
                    style = AppTheme.typography.label3,
                    color = LocalContentColor.current.copy(ContentAlpha.medium),
                    modifier = Modifier.sharedElement(RouteConsole.ID_SUBTITLE),
                )
            }
        },

        // AlbumArt
        trailing = {
            AsyncImage(
                model = state.artwork,
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .shadow(4.dp, DefaultArtworkShape)
                    .background(AppTheme.colors.background(1.dp))
                    .size(DefaultArtworkSize),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        },

        // Controls
        subheading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = CP.medium).fillMaxWidth(),
                content = {
                    val bgModifier = Modifier
                        .scale(0.85f)
                        .background(AppTheme.colors.onAccent.copy(0.3f), CircleShape)
                    // SeekBackward
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        modifier =  Modifier.sharedElement(RouteConsole.ID_BTN_SKIP_PREVIOUS) then bgModifier
                    )

                    // Play/Pause
                    FloatingActionButton(
                        onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
                        shape = AppTheme.shapes.large,
                        backgroundColor = AppTheme.colors.onAccent.copy(0.3f),
                        contentColor = LocalContentColor.current,
                        modifier = Modifier
                            .sharedElement(RouteConsole.ID_BTN_PLAY_PAUSE)
                            .scale(0.9f),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        content = {
                            Icon(
                                painter = lottieAnimationPainter(
                                    id = R.raw.lt_play_pause,
                                    atEnd = state.playing,
                                    progressRange = 0.0f..0.29f,
                                    animationSpec = tween(easing = LinearEasing)
                                ),
                                modifier = Modifier.lottie(1.5f),
                                contentDescription = null
                            )
                        }
                    )

                    // SeekNext
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_SKIP_TO_NEXT) then bgModifier
                    )

                    // Open in console
                    IconButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                        modifier = bgModifier,
                    )
                }
            )
        }
    )
}