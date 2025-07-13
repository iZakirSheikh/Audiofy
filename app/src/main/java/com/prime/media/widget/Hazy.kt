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

package com.prime.media.widget

import android.text.format.DateUtils
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prime.media.R
import com.prime.media.common.compose.background
import com.prime.media.common.compose.chronometer
import com.prime.media.common.compose.lottie
import com.prime.media.common.compose.lottieAnimationPainter
import com.prime.media.common.compose.marque
import com.prime.media.common.compose.shine
import com.prime.media.console.RouteConsole
import com.zs.compose.foundation.background
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
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import dev.chrisbanes.haze.HazeState

private const val TAG = "Hazy"

@Composable
fun Hazy(
    state: NowPlaying,
    surface: HazeState,
    onAction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val onColor = AppTheme.colors.onBackground
    // The position (-1 if N/A) animated from 0; stops at duration (if not N/A)
    val position = state.chronometer
    // content
    BaseListItem(
        centerAlign = true,
        modifier = modifier
            .sharedBounds(RouteConsole.SHARED_ELEMENT_BACKGROUND)
            .border(AppTheme.colors.shine, AppTheme.shapes.large)
            .shadow(8.dp, AppTheme.shapes.large)
            .background(AppTheme.colors.background(surface)),
        contentColor = onColor,
        // Title as heading
        heading = {
            Label(
                state.title ?: textResource(R.string.unknown),
                style = AppTheme.typography.title1,
                modifier = Modifier
                    .sharedElement(RouteConsole.SHARED_ELEMENT_TITLE)
                    .marque(Int.MAX_VALUE),
                color = onColor,
                fontWeight = FontWeight.Bold
            )
        },
        // Subtitle as subheading
        subheading = {
            Label(
                state.subtitle ?: textResource(R.string.unknown),
                style = AppTheme.typography.label3,
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                modifier = Modifier.sharedElement(RouteConsole.SHARED_ELEMENT_SUBTITLE),
            )
        },
        // Duration
        overline = {
            val fPosition =
                if (position == Remote.TIME_UNSET) "N/A" else DateUtils.formatElapsedTime(position / 1000)
            val fDuration =
                if (state.duration == Remote.TIME_UNSET) "N/A" else DateUtils.formatElapsedTime(
                    state.duration / 1000
                )
            Label(
                "$fPosition | $fDuration",
                style = AppTheme.typography.label3,
                color = onColor.copy(ContentAlpha.medium),
            )
        },
        // Artwork
        leading = {
            AsyncImage(
                model = state.artwork,
                modifier = Modifier
                    .sharedElement(RouteConsole.SHARED_ELEMENT_ARTWORK)
                    .clip(AppTheme.shapes.medium)
                    .size(84.dp),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        },
        // Play Toggle
        trailing = {
            FloatingActionButton(
                onClick = { onAction(MiniPlayer.ACTION_PLAY_TOGGLE) },
                shape = AppTheme.shapes.large,
                modifier = Modifier.scale(0.9f),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                content = {
                    Icon(
                        painter = lottieAnimationPainter(
                            id = R.raw.lt_play_pause,
                            atEnd = state.playing,
                            progressRange = 0.0f..0.29f,
                            animationSpec = tween(easing = LinearEasing)
                        ),
                        modifier = Modifier
                            .sharedElement(RouteConsole.SHARED_ELEMENT_CONTROLS)
                            .lottie(1.5f),
                        contentDescription = null
                    )
                }
            )
        },
        // Progress
        footer = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Spacer(Modifier.sharedElement(RouteConsole.SHARED_ELEMENT_PLAYING_BARS))
                    // Skip to previous
                    IconButton(
                        onClick = { onAction(MiniPlayer.ACTION_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                    )

                    // Slider
                    Slider(
                        if (position == Remote.TIME_UNSET || state.duration == Remote.TIME_UNSET) 1f else position.toFloat() / state.duration,
                        onValueChange = onAction,
                        modifier = Modifier.weight(1f),
                    )

                    // SeekNext
                    IconButton(
                        onClick = { onAction(MiniPlayer.ACTION_SKIP_TO_NEXT) },
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                    )

                    // Expand to fill
                    IconButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        onClick = { onAction(MiniPlayer.ACTION_OPEN_CONSOLE) },
                    )
                }
            )
        }
    )
}