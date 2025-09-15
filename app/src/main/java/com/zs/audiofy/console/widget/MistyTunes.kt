/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 07-07-2025.
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

package com.zs.audiofy.console.widget

import android.text.format.DateUtils
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.console.RouteConsole
import com.zs.compose.foundation.background
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.FloatingActionButton
import com.zs.compose.theme.FloatingActionButtonDefaults
import com.zs.compose.theme.Icon
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.Slider
import com.zs.compose.theme.SliderDefaults
import com.zs.compose.theme.TonalIconButton
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import dev.chrisbanes.haze.HazeState

private const val TAG = "MistyTunes"

/**
 * Represents an inApp Widget that features background blur.
 */
@Composable
fun MistyTunes(
    state: NowPlaying,
    surface: HazeState,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val onColor = AppTheme.colors.onBackground
    // The position (Long.MIN_VALUE if N/A) animated from 0; stops at duration (if not N/A)
    val chronometer = state.chronometer
    // Layout
    BaseListItem(
        centerAlign = true,
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .shadow(8.dp, AppTheme.shapes.large)
            .border(AppTheme.colors.shine, AppTheme.shapes.large)
            .background(AppTheme.colors.background(surface)),
        contentColor = onColor,
        // Title as heading
        heading = {
            Box(
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_TITLE)
                    .clipToBounds(),
                content = {
                    Label(
                        state.title ?: textResource(R.string.unknown),
                        style = AppTheme.typography.title1,
                        modifier = Modifier
                            .marque(Int.MAX_VALUE),
                        color = onColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        // Subtitle as subheading
        subheading = {
            Label(
                state.subtitle ?: textResource(R.string.unknown),
                style = AppTheme.typography.label3,
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                modifier = Modifier.sharedElement(RouteConsole.ID_SUBTITLE),
            )
        },
        // Extra
        overline = {
            Label(
                style = AppTheme.typography.label3,
                color = onColor.copy(ContentAlpha.medium),
                modifier = Modifier.sharedElement(RouteConsole.ID_EXTRA_INFO),
                text = buildString {
                    val elapsed = chronometer.elapsed
                    val fPos =
                        if (elapsed == Long.MIN_VALUE) "N/A" else DateUtils.formatElapsedTime(
                            elapsed / 1000
                        )
                    val duration = state.duration
                    val fDuration =
                        if (duration == Remote.TIME_UNSET) "N/A" else DateUtils.formatElapsedTime(
                            duration / 1000
                        )
                    append(
                        "$fPos / $fDuration (${
                            stringResource(
                                R.string.postfix_x_f,
                                state.speed
                            )
                        })"
                    )
                }
            )
        },
        // Artwork
        // TODO - Maybe use Crossfire
        leading = {
            AsyncImage(
                model = state.artwork,
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .clip(AppTheme.shapes.medium)
                    .background(AppTheme.colors.background(1.dp))
                    .size(84.dp),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        },
        // Play Toggle
        trailing = {
            FloatingActionButton(
                onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
                shape = AppTheme.shapes.large,
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
        // Progress
        footer = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth(),
                content = {
                    // Placeholder for playingbars
                    Spacer(Modifier.sharedElement(RouteConsole.ID_PLAYING_INDICATOR))
                    // Like
                    val size = Modifier.size(35.dp).scale(0.9f)
                    LottieAnimatedButton(
                        R.raw.lt_twitter_heart_filled_unfilled,
                        onClick = { onRequest(Widget.REQUEST_LIKED) },
                        animationSpec = tween(800),
                        atEnd = state.favourite, // if fav
                        contentDescription = null,
                        progressRange = 0.13f..1.0f,
                        scale = 3.5f,
                        tint = AppTheme.colors.accent,
                        modifier = Modifier.layoutId(RouteConsole.ID_BTN_LIKED) then size
                    )
                    val tint = AppTheme.colors.accent
                    // Skip to previous
                    TonalIconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        color = tint,
                        enabled = state.isPrevAvailable,
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_SKIP_PREVIOUS) then size
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
                            disabledThumbColor = AppTheme.colors.accent,
                            disabledActiveTrackColor = AppTheme.colors.accent
                        )
                    )
                    // SeekNext
                    TonalIconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        color = tint,
                        enabled = state.isNextAvailable,
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_SKIP_TO_NEXT) then size
                    )
                    // Expand to fill
                    TonalIconButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                        color = tint,
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_COLLAPSE) then size
                    )
                }
            )
        }
    )
}