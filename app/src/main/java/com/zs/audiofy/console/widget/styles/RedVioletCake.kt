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

import android.text.format.DateUtils
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.foreground
import com.zs.compose.foundation.textResource
import com.zs.compose.foundation.thenIf
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.Colors
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.Slider
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote

private val WidgetShape = RoundedCornerShape(14)

private val Colors.widgetBackground
    @Composable
    inline get() = background(2.dp)
private val Colors.veil
    @Composable
    inline get() = Brush.horizontalGradient(
        0.0f to widgetBackground,
        0.4f to widgetBackground.copy(0.85f),
        0.96f to Color.Transparent,
        //startX = -30f
    )


/**
 * Represents a widget inspired from the media notification of android 11.
 */
@Composable
fun RedVioletCake(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val background = colors.widgetBackground

    Box(
        modifier = modifier
            .shadow(12.dp, WidgetShape)
            .thenIf(!colors.isLight) { border(0.5.dp, colors.accent.copy(0.12f), WidgetShape) }
            .background(background, WidgetShape)
            .heightIn(max = 150.dp)
            .fillMaxWidth(),
        content = {
            // AlbumArt - The artwork situated in end of the component
            AsyncImage(
                model = state.artwork,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .align(Alignment.TopEnd)
                    .aspectRatio(1.0f, matchHeightConstraintsFirst = true)
                    .foreground(colors.veil)
                    .foreground(Color.Black.copy(0.24f))
                    .clip(WidgetShape),
            )

            // actual content
            val accent = colors.accent
            val contentColor = AppTheme.colors.onBackground
            BaseListItem(
                contentColor = contentColor,
                modifier = Modifier.sharedBounds(RouteConsole.ID_BACKGROUND),

                // Subtitle
                heading = {
                    Label(
                        state.subtitle ?: stringResource(R.string.unknown),
                        color = contentColor.copy(ContentAlpha.medium),
                        style = AppTheme.typography.label3,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                },
                // Title
                overline = {
                    Box(
                        modifier = Modifier
                            .sharedElement(RouteConsole.ID_TITLE)
                            .clipToBounds(),
                        content = {
                            Label(
                                state.title ?: textResource(R.string.unknown),
                                style = AppTheme.typography.title2,
                                modifier = Modifier
                                    .marque(Int.MAX_VALUE),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                },
                // Music Bars
                trailing = {
                    Icon(
                        painter = lottieAnimationPainter(
                            R.raw.playback_indicator,
                            isPlaying = state.playing
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .sharedElement(RouteConsole.ID_PLAYING_INDICATOR)
                            .lottie(),
                        tint = accent
                    )
                },
                // Controls
                subheading = {
                    Row(
                        horizontalArrangement = ContentPadding.xSmallArrangement,
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
                                id = R.raw.lt_play_pause,
                                atEnd = state.playing,
                                scale = 1.8f,
                                progressRange = 0.0f..0.29f,
                                animationSpec = tween(easing = LinearEasing),
                                onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.sharedElement(RouteConsole.ID_BTN_PLAY_PAUSE)
                            )

                            // SeekNext
                            IconButton(
                                onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                                icon = Icons.Outlined.KeyboardDoubleArrowRight,
                                contentDescription = null,
                                tint = contentColor
                            )

                            Spacer(Modifier.width(ContentPadding.medium))

                            // Like Button
                            LottieAnimatedButton(
                                R.raw.lt_twitter_heart_filled_unfilled,
                                onClick = { onRequest(Widget.REQUEST_LIKED) },
                                animationSpec = tween(800),
                                atEnd = state.favourite, // if fav
                                contentDescription = null,
                                progressRange = 0.13f..1.0f,
                                scale = 3.5f,
                                tint = accent,
                                modifier = Modifier.layoutId(RouteConsole.ID_BTN_LIKED) then Widget.SmallIconBtn
                            )
                        }
                    )
                },
                // Progress
                footer = {
                    Row(
                        horizontalArrangement = ContentPadding.xSmallArrangement,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            // control centre
                            IconButton(
                                icon = Icons.Outlined.Tune,
                                contentDescription = null,
                                onClick = { onRequest(Widget.REQUEST_SHOW_CONFIG) },
                                tint = contentColor
                            )

                            // played duration
                            val chronometer = state.chronometer
                            val position = chronometer.elapsed
                            Label(
                                when (position) {
                                    Long.MIN_VALUE -> stringResource(R.string.abbr_not_available)
                                    else -> DateUtils.formatElapsedTime((position / 1000))
                                },
                                style = AppTheme.typography.label3,
                                color = contentColor.copy(ContentAlpha.medium)
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
                                    .weight(1f)
                            )

                            // total duration
                            val duration = state.duration
                            Label(
                                when (duration) {
                                    Remote.TIME_UNSET -> stringResource(R.string.abbr_not_available)
                                    else -> DateUtils.formatElapsedTime((duration / 1000))
                                },
                                style = AppTheme.typography.label3,
                                color = contentColor.copy(ContentAlpha.medium)
                            )

                            // control centre
                            IconButton(
                                icon = Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                                tint = Color.SignalWhite
                            )
                        }
                    )
                }
            )
        }
    )
}