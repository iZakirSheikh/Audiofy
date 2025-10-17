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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.shapes.SkewedRoundedRectangleShape
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.Slider
import com.zs.compose.theme.SliderDefaults
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote

private val WidgetShape = SkewedRoundedRectangleShape(15.dp, 0.15f)
private val ArtworkShape = SkewedRoundedRectangleShape(15.dp)
private val ArtworkSize = 84.dp
private val TitleDrawStyle =
    Stroke(width = 3.0f /*pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)*/)


@Composable
fun SkewedDynamic(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val contentColor = colors.onBackground
    BaseListItem(
        contentColor = contentColor,
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .shadow(16.dp, WidgetShape)
            .border(0.5.dp, colors.accent.copy(if (colors.isLight) 0.24f else 0.12f), WidgetShape)
            .background(AppTheme.colors.background(1.dp)),
        // subtitle
        heading = {
            Label(
                state.subtitle ?: stringResource(R.string.unknown),
                style = AppTheme.typography.label3,
                color = contentColor.copy(ContentAlpha.medium)
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
                        modifier = Modifier.marque(Int.MAX_VALUE),
                        style = AppTheme.typography.title2,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        // AlbumArt
        leading = {
            AsyncImage(
                model = state.artwork,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .size(ArtworkSize)
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .clip(ArtworkShape)
                    .border(1.dp, colors.onBackground, ArtworkShape)
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

                IconButton(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                    modifier = Modifier.offset(10.dp, -4.dp)
                )
            }
        },
        // play controls
        subheading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    // SeekBackward
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        tint = contentColor
                    )

                    // Play/Pause
                    LottieAnimatedButton(
                        id = R.raw.lt_play_pause5,
                        atEnd = state.playing,
                        scale = 2.5f,
                        progressRange = 0.0f..0.45f,
                        animationSpec = tween(easing = LinearEasing),
                        onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
                        contentDescription = null,
                        tint = contentColor
                    )

                    // SeekNext
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                        contentDescription = null,
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        tint = contentColor
                    )
                }
            )
        },
        // progress
        footer = {
            Row(
                horizontalArrangement = ContentPadding.SmallArrangement,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    val chronometer = state.chronometer
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
                        ),
                        modifier = Modifier.animateContentSize()
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
                            thumbColor = contentColor,
                            activeTrackColor = contentColor,
                            disabledThumbColor = contentColor,
                            disabledInactiveTrackColor = contentColor
                        )
                    )

                    // Duration
                    val duration = state.duration
                    Label(
                        when {
                            duration == Remote.TIME_UNSET -> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime(((duration) / 1000))
                        },
                        style = AppTheme.typography.label3,
                        color = contentColor.copy(ContentAlpha.medium),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    )
}