/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 01-03-2025.
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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.shapes.SkewedRoundedRectangleShape
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.UmbraGrey
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.Slider
import com.zs.compose.theme.SliderDefaults
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying

private const val TAG = "RotatingGradient"

private val Shape = RoundedCornerShape(12)
private val ArtworkSize = 84.dp
private val ArtworkShape = /*RoundedPolygonShape(5, 0.3f)*/SkewedRoundedRectangleShape(15.dp)
private val TitleDrawStyle = Stroke(width = 3f, join = StrokeJoin.Round)

private val PositionTextShadow = Shadow(
    offset = Offset(3f, 3f),  // You can adjust the shadow's offset
    blurRadius = 10f  // You can adjust the blur radius
)

@Composable
fun MistyDream(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = Color.UmbraGrey
    // Content
    BaseListItem(
        contentColor = contentColor,
        spacing = ContentPadding.small,
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .shadow(12.dp, Shape)
            .border(1.dp, Color.Gray.copy(0.24f), Shape)
            .background(Color.SignalWhite)
            .background(
                lottieAnimationPainter(R.raw.lt_bg_blur),
                contentScale = ContentScale.Crop
            ),
        // subtitle
        overline = {
            Label(
                state.subtitle ?: textResource(R.string.unknown),
                style = AppTheme.typography.label3,
                color = contentColor.copy(ContentAlpha.medium),
                modifier = Modifier.sharedElement(RouteConsole.ID_SUBTITLE),
            )
        },
        // Title
        heading = {
            Box(
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_TITLE)
                    .clipToBounds(),
                content = {
                    Label(
                        state.title ?: textResource(R.string.unknown),
                        modifier = Modifier.marque(Int.MAX_VALUE),
                        fontWeight = FontWeight.Bold,
                        style = AppTheme.typography.headline2.copy(
                            drawStyle = TitleDrawStyle,
                            fontWeight = FontWeight.Medium,
                        )
                    )
                }
            )
        },
        // album art
        leading = {
            AsyncImage(
                model = state.artwork,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .size(ArtworkSize)
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .clip(ArtworkShape)
                    .border(1.dp, contentColor, ArtworkShape),
            )
        },
        // Expand to fill
        trailing = {
            IconButton(
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                modifier = Modifier
                    .scale(0.9f)
                    .offset(x = 14.dp),
            )
        },
        // Controls
        subheading = {
            Row(
                horizontalArrangement = ContentPadding.xSmallArrangement,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    // Skip to Prev
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        tint = contentColor
                    )

                    // Play/Pause
                    LottieAnimatedButton(
                        id = R.raw.lt_play_pause8,
                        atEnd = state.playing,
                        scale = 5f,
                        progressRange = 0.0f..0.75f,
                        animationSpec = tween(easing = LinearEasing),
                        onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_PLAY_PAUSE)
                    )

                    // Skip to Next
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
            )
        },
        // progress
        footer = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    // PlayingBars
                    Icon(
                        painter = lottieAnimationPainter(R.raw.playback_indicator, isPlaying = state.playing),
                        contentDescription = null,
                        modifier = Modifier
                            .sharedElement(RouteConsole.ID_PLAYING_INDICATOR)
                            .lottie(),
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
                            disabledThumbColor = contentColor,
                            disabledActiveTrackColor = contentColor,
                            thumbColor = contentColor,
                            activeTrackColor = contentColor
                        )
                    )
                    // Position
                    val position = chronometer.elapsed
                    Label(
                        when (position) {
                            Long.MIN_VALUE -> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime((position / 1000))
                        },
                        style = AppTheme.typography.headline2.copy(
                            drawStyle = TitleDrawStyle,
                            fontWeight = FontWeight.Medium,
                            //  shadow = PositionTextShadow
                        ),
                        modifier = Modifier.animateContentSize()
                    )
                }
            )
        }
    )
}
