@file:OptIn(ExperimentalSharedTransitionApi::class)

/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 25-02-2025.
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

package com.zs.audiofy.console.widget.styles

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.shapes.RoundedPolygonShape
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.ContentAlpha
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

private const val TAG = "WavyGradeintDots"

private val Shape = RoundedCornerShape(12)
private val ArtworkSize = 84.dp
private val ArtworkShape = RoundedPolygonShape(5, 0.3f)
private val TitleDrawStyle = Stroke(width = 3.2f, join = StrokeJoin.Round)

private val IconModifier = Modifier
    .scale(0.84f)
    .background(Color.SignalWhite.copy(0.3f), CircleShape)

@Composable
fun WavyGradientDots(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BaseListItem(
        contentColor = Color.SignalWhite,
        spacing = 0.dp,
        centerAlign = true,
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .shadow(8.dp, Shape)
            .border(1.dp, Color.Gray.copy(0.24f), Shape)
            .background(
                lottieAnimationPainter(R.raw.bg_gradeint_dots),
                contentScale = ContentScale.Crop,
                overlay = Color.Black.copy(0.3f)
            ),
        // subtitle
        heading = {
            Label(
                state.subtitle ?: stringResource(R.string.unknown),
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                style = AppTheme.typography.label3,
                modifier = Modifier.fillMaxWidth(0.85f) then Modifier.sharedElement(RouteConsole.ID_SUBTITLE),
            )
        },
        // title
        subheading = {
            Box(
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_TITLE)
                    .clipToBounds(),
                content = {
                    Label(
                        state.title ?: textResource(R.string.unknown),
                        modifier = Modifier.marque(Int.MAX_VALUE),
                        style = AppTheme.typography.headline3.copy(
                            drawStyle = TitleDrawStyle,
                            fontWeight = FontWeight.Medium
                        )
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
                    .shadow(4.dp, ArtworkShape)
                    .background(AppTheme.colors.background(1.dp))
                    .size(ArtworkSize),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        },
        // Play/Pause
        trailing = {
            val radius by animateIntAsState(if (state.playing) 28 else 100)
            FloatingActionButton(
                onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
                shape = RoundedCornerShape(radius),
                backgroundColor = LocalContentColor.current.copy(ContentAlpha.disabled),
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
        },
        // controls
        footer = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
// Expand to fill
                    val color = LocalContentColor.current.copy(ContentAlpha.medium)
                    // SeekBackward
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        tint = color,
                        modifier = IconModifier
                    )

                    val chronometer = state.chronometer
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
                            disabledThumbColor = LocalContentColor.current,
                            disabledActiveTrackColor = LocalContentColor.current,
                            thumbColor = LocalContentColor.current,
                            activeTrackColor = LocalContentColor.current
                        )
                    )

                    // SeekNext
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT)},
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        tint = color,
                        modifier = IconModifier
                    )

                    // Expand to fill
                    IconButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                        modifier = IconModifier
                    )
                }
            )
        }
    )
}
