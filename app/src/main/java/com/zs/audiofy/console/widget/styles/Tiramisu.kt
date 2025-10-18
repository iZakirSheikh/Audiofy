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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.UmbraGrey
import com.zs.compose.foundation.blend
import com.zs.compose.foundation.foreground
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.Colors
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.FloatingActionButton
import com.zs.compose.theme.FloatingActionButtonDefaults
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.Slider
import com.zs.compose.theme.SliderDefaults
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying

private val TiramisuShape = RoundedCornerShape(14)
private inline val Colors.ring
    @Composable
    get() =
        Brush.horizontalGradient(listOf(accent.copy(0.5f), Color.Transparent, accent.copy(0.5f)))
private inline val Colors.contentColor
    @Composable get() =
        if (accent.luminance() > 0.6f) Color.UmbraGrey else Color.SignalWhite

/**
 * Represents a widget inspired from the media notification of android 13.
 */
@Composable
fun Tiramisu(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .shadow(12.dp, TiramisuShape)
            .background(AppTheme.colors.background)
            .heightIn(max = 160.dp)
            .fillMaxWidth(),
        content = {
            // AlbumArt
            AsyncImage(
                model = state.artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .clip(TiramisuShape)
                    .foreground(colors.ring)
                    .foreground(Color.Black.copy(0.26f))
                    .matchParentSize(),
            )

            val contentColor = Color.SignalWhite
            BaseListItem(
                contentColor = contentColor,
                centerAlign = true,
                // title
                subheading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .sharedElement(RouteConsole.ID_TITLE)
                            .clipToBounds(),
                        content = {
                            Label(
                                state.title ?: stringResource(R.string.unknown),
                                modifier = Modifier.marque(Int.MAX_VALUE),
                                color = contentColor.copy(ContentAlpha.medium),
                                style = AppTheme.typography.label3,
                            )
                        }
                    )
                },
                // subtitle
                heading = {
                    Label(
                        state.title ?: stringResource(R.string.unknown),
                        style = AppTheme.typography.title2,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        fontWeight = FontWeight.Bold,
                    )
                },
                // Play/Pause
                trailing = {
                    // Play/Pause
                    FloatingActionButton(
                        backgroundColor = Color.SignalWhite.blend(colors.accent, 0.2f),
                        contentColor = Color.UmbraGrey,
                        shape = RoundedCornerShape(28),
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_PLAY_PAUSE),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
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
                        },
                    )
                },
                // control centre
                overline = {
                    IconButton(
                        icon = Icons.Outlined.Tune,
                        contentDescription = null,
                        onClick = { onRequest(Widget.REQUEST_SHOW_CONFIG) },
                        modifier = Modifier.offset(-12.dp, -16.dp)
                    )
                },
                // Progress
                footer = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            // Expand to fill
                            val tint = contentColor.copy(ContentAlpha.medium)
                            // SeekBackward
                            IconButton(
                                onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                                icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                                contentDescription = null,
                                tint = tint
                            )

                            // Slider
                            val chronometer = state.chronometer
                            Slider(
                                chronometer.progress(state.duration),
                                onValueChange = {
                                    val mills = (it * state.duration).toLong()
                                    chronometer.raw = mills
                                },
                                onValueChangeFinished = {
                                    val progress = chronometer.elapsed / state.duration.toFloat()
                                    onRequest(progress)
                                },
                                enabled = state.duration > 0,
                                modifier = Modifier
                                    .sharedElement(RouteConsole.ID_SEEK_BAR)
                                    .weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = contentColor,
                                    activeTrackColor = contentColor,
                                    disabledThumbColor = contentColor,
                                    inactiveTrackColor = contentColor,
                                    disabledActiveTrackColor = contentColor
                                )
                            )
                            // SeekNext
                            IconButton(
                                onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                                icon = Icons.Outlined.KeyboardDoubleArrowRight,
                                contentDescription = null,
                                tint = tint
                            )

                            // Expand to fill
                            IconButton(
                                contentDescription = null,
                                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                                onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                            )
                        }
                    )
                }
            )
        }
    )
}