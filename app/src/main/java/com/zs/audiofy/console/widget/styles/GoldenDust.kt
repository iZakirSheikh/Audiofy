/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 05-09-2024.
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

import android.text.format.DateUtils
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.zs.audiofy.common.shapes.RoundedPolygonShape
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.effects.shimmer
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.FloatingActionButton
import com.zs.compose.theme.FloatingActionButtonDefaults
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.Slider
import com.zs.compose.theme.SliderDefaults
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import com.zs.audiofy.common.compose.ContentPadding as CP

private val WidgetShape = RoundedCornerShape(16.dp)
private val ArtworkShape = RoundedPolygonShape(6, 0.3f)
private val DefaultArtworkSize = 78.dp
private val ShimmerAnimSpec =
    infiniteRepeatable<Float>(tween(5000, 2500, easing = EaseInOutBounce))

private val Accent = Color(0xCBFF9405)
private val onAccent = Color(0xFFFED68C)

private val bgColor = Color(0xFF1F1B11)
private val background = Brush.linearGradient(
    0.0f to Accent.copy(0.5f),
    0.55f to bgColor,
    start = Offset.Infinite,
    end = Offset.Zero
)
private val IconModifier = Modifier
    .scale(0.84f)
    .background(onAccent.copy(0.3f), CircleShape)
private val PlayButtonShape = RoundedCornerShape(28)

@Composable
fun GoldenDust(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BaseListItem(
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .visualEffect(ImageBrush.NoiseBrush, 0.4f, overlay = true)
            .shimmer(Accent, 400.dp, BlendMode.Overlay, ShimmerAnimSpec)
            .border(1.dp, onAccent, WidgetShape)
            .background(bgColor, WidgetShape)
            .background(background, WidgetShape),
        contentColor = onAccent,
        // Title
        heading = {
            Box(
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_TITLE)
                    .clipToBounds(),
                content = {
                    Label(
                        state.title ?: stringResource(R.string.unknown),
                        modifier = Modifier.marque(Int.MAX_VALUE),
                        style = AppTheme.typography.headline3,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },

        // Supporting text
        overline = {
            Label(
                state.subtitle ?: stringResource(R.string.unknown),
                style = AppTheme.typography.label3,
                color = LocalContentColor.current,
                modifier = Modifier.sharedElement(RouteConsole.ID_SUBTITLE),
            )
        },

        // Artwork
        trailing = {
            AsyncImage(
                model = state.artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(DefaultArtworkSize)
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .scale(1.15f)
                    .shadow(8.dp, ArtworkShape),
            )
        },

        // Controls
        subheading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = CP.medium)
                    .fillMaxWidth(),
                content = {
                    // SeekBackward
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_SKIP_PREVIOUS) then IconModifier
                    )

                    // Play/Pause
                    FloatingActionButton(
                        backgroundColor = onAccent.copy(0.3f),
                        contentColor = LocalContentColor.current,
                        shape = PlayButtonShape,
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

                    // SeekNext
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_SKIP_TO_NEXT) then Modifier.padding(
                            end = CP.normal
                        ) then IconModifier
                    )

                    // control centre
                    IconButton(
                        contentDescription = null,
                        icon = Icons.Outlined.Tune,
                        onClick = { onRequest(Widget.REQUEST_SHOW_CONFIG) },
                        modifier = IconModifier
                    )
                }
            )
        },

        // progress
        footer = {
            Row(
                horizontalArrangement = CP.xSmallArrangement,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    // Playing bars.
                    Icon(
                        painter = lottieAnimationPainter(
                            R.raw.playback_indicator,
                            isPlaying = state.playing
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = CP.small)
                            .sharedElement(RouteConsole.ID_PLAYING_INDICATOR)
                            .lottie(),
                        tint = onAccent
                    )

                    // position
                    val chronometer = state.chronometer
                    val position = chronometer.elapsed
                    Label(
                        when (position) {
                            Long.MIN_VALUE -> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime((position / 1000L))
                        },
                        style = AppTheme.typography.label3,
                    )
                    // Slider
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
                            thumbColor = Accent,
                            activeTrackColor = Accent,
                            disabledThumbColor = Accent,
                            inactiveTrackColor = Accent,
                            disabledActiveTrackColor = Accent
                        )
                    )

                    // total duration
                    Label(
                        when (state.duration) {
                            Remote.TIME_UNSET -> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime((state.duration / 1000))
                        },
                        style = AppTheme.typography.label3,
                        color = LocalContentColor.current,
                    )

                    // Expand to fill
                    IconButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_COLLAPSE) then IconModifier
                    )
                }
            )
        }
    )
}