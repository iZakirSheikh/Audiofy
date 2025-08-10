/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 07-08-2025.
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

package com.zs.audiofy.console

import android.text.format.DateUtils
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.compose.rememberAnimatedVectorPainter
import com.zs.audiofy.common.compose.shine
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote

private const val TAG = "PlayerView"

/** Represents [NowPlaying] default state in [Console] */
private val NonePlaying = NowPlaying(null, null)

/**
 * Displays the [PlayerView] within the console interface.
 *
 * @param background The visual style of the console’s background.
 *                   For example, use [Console.STYLE_BG_SIMPLE] for a minimal appearance.
 * @param onNewAction Callback invoked when the user performs a standard action in the [Console].
 *                    Return `true` to indicate that the action has been handled,
 *                    or `false` to allow further processing.
 */
@Composable
fun PlayerView(
    viewState: ConsoleViewState,
    background: Int,
    insets: WindowInsets,
    onNewAction: (code: Int) -> Boolean,
    modifier: Modifier = Modifier
) {
    // collect the state.
    val _state by viewState.state.collectAsState()
    val state = _state ?: NonePlaying
    // calculate the new constraints
    val clazz = LocalWindowSize.current
    val ldr = LocalLayoutDirection.current
    val density = LocalDensity.current
    val constraints = remember(clazz, insets) {
        val dpInsets = with(density) {
            // Convert raw insets to dp values, considering layout direction
            DpRect(
                left = insets.getLeft(density, ldr).toDp(),
                right = insets.getRight(this, ldr).toDp(),
                top = insets.getTop(this).toDp(),
                bottom = insets.getBottom(this).toDp()
            )
        }
        calculateConstraintSet(clazz, dpInsets, false, false)
    }
    // Build The layout
    ConstraintLayout(
        constraintSet = constraints.value,
        modifier = modifier,
        animateChangesSpec = AppTheme.motionScheme.defaultEffectsSpec(),
        content = {
            // Background
            Spacer(
                modifier = Modifier
                    .background(AppTheme.colors.background(1.dp))
                    .layoutId(Console.ID_BACKGROUND)
            )
            val contentColor = LocalContentColor.current

            // Collapse
            IconButton(
                icon = Icons.Outlined.ExpandMore,
                onClick = { },
                modifier = Modifier
                    .border(AppTheme.colors.shine, CircleShape)
                    .background(AppTheme.colors.background(3.dp), shape = CircleShape)
                    .layoutId(Console.ID_BTN_COLLAPSE),
                tint = AppTheme.colors.accent,
                contentDescription = null,
            )

            // Artwork
            Artwork(
                model = state.artwork,
                modifier = Modifier.layoutId(Console.ID_ARTWORK),
                border = 3.dp,
                shape = AppTheme.shapes.xLarge,
                shadow = 4.dp
            )

            // Title
            Label(
                text = state.title ?: stringResource(id = R.string.unknown),
                fontSize = constraints.titleTextSize,// Maybe Animate
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .marque(Int.MAX_VALUE)
                    .layoutId(Console.ID_TITLE),
                color = contentColor
            )

            // Subtitle
            Label(
                text = state.subtitle ?: stringResource(id = R.string.unknown),
                style = AppTheme.typography.label3,
                modifier = Modifier
                    .sharedElement(Console.ID_SUBTITLE)
                    .layoutId(Console.ID_SUBTITLE),
                color = contentColor.copy(ContentAlpha.medium)
            )

            // Position
            val chronometer = state.chronometer
            val elapsed = chronometer.elapsed
            val fPos =
                if (elapsed == Long.MIN_VALUE) "N/A" else DateUtils.formatElapsedTime(elapsed / 1000)
            val duration = state.duration
            val fDuration =
                if (duration == Remote.TIME_UNSET) "N/A" else DateUtils.formatElapsedTime(duration / 1000)
            Label(
                "$fPos / $fDuration (${stringResource(R.string.postfix_x_f, state.speed)})",
                style = AppTheme.typography.label3,
                color = contentColor.copy(ContentAlpha.medium),
                modifier = Modifier.layoutId(Console.ID_POSITION),
            )

            // Slider
            TimeBar(
                progress =  chronometer.progress(state.duration),
                onValueChange = {
                    val mills = (it * state.duration).toLong()
                    chronometer.raw = mills
                },
                onValueChangeFinished = {
                    val progress = chronometer.elapsed / state.duration.toFloat()
                    viewState.seekTo(progress)
                },
                modifier = Modifier.layoutId(Console.ID_SEEK_BAR),
                enabled = state.duration > 0,
            )

            // Shuffle
            LottieAnimatedButton(
                id = R.raw.lt_shuffle_on_off,
                onClick = { viewState.shuffle(!state.shuffle) },
                atEnd = state.shuffle,
                progressRange = 0f..0.8f,
                scale = 1.5f,
                contentDescription = null,
                tint = if (state.shuffle) AppTheme.colors.accent else contentColor.copy(ContentAlpha.disabled),
                modifier = Modifier.layoutId(Console.ID_SHUFFLE)
            )

            // Skip to next
            IconButton(
                onClick = viewState::skipToNext,
                icon = Icons.Outlined.KeyboardDoubleArrowRight,
                contentDescription = null,
                enabled = true,
                modifier = Modifier.layoutId(Console.ID_BTN_SKIP_TO_NEXT)
            )

            // Skip to Prev
            IconButton(
                onClick = viewState::skipToPrev,
                icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                contentDescription = null,
                enabled = true,
                modifier = Modifier.layoutId(Console.ID_BTN_SKIP_PREVIOUS)
            )

            // Repeat Mode
            IconButton(
                onClick = viewState::cycleRepeatMode,
                content = {
                    val mode = state.repeatMode
                    Icon(
                        painter = rememberAnimatedVectorPainter(R.drawable.avd_repeat_more_one_all, mode == Remote.REPEAT_MODE_ALL),
                        contentDescription = null,
                        tint = contentColor.copy(if (mode == Remote.REPEAT_MODE_OFF) ContentAlpha.disabled else ContentAlpha.high)
                    )
                },
                modifier = Modifier.layoutId(Console.ID_BTN_REPEAT_MODE)
            )

            PlayButton(
                onClick = viewState::togglePlay,
                isPlaying = state.playing,
                modifier = Modifier.layoutId(Console.ID_BTN_PLAY_PAUSE)
            )

            // Resize Mode
            IconButton(
                icon = Icons.Outlined.Fullscreen,
                contentDescription = null,
                onClick = {},
                enabled = state.isVideo,
                modifier = Modifier.layoutId(Console.ID_BTN_RESIZE_MODE)
            )

            // rotation
            IconButton(
                icon = Icons.Outlined.ScreenRotation,
                contentDescription = null,
                onClick = { onNewAction(Console.ACTION_TOGGLE_ROTATION_LOCK) },
                modifier = Modifier.layoutId(Console.ID_BTN_ROTATION_LOCK)
            )

            // Queue
            IconButton(
                icon = Icons.Outlined.Queue,
                contentDescription = null,
                onClick = { onNewAction(Console.ACTION_SHOW_QUEUE) },
                modifier = Modifier.layoutId(Console.ID_BTN_QUEUE)
            )

            IconButton(
                icon = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                onClick = {},
                modifier = Modifier.layoutId(Console.ID_BTN_LIKED)
            )

            IconButton(
                icon = Icons.Outlined.Speed,
                contentDescription = null,
                onClick = {},
                modifier = Modifier.layoutId(Console.ID_BTN_PLAYBACK_SPEED)
            )

            IconButton(
                icon = Icons.Outlined.Timer,
                contentDescription = null,
                onClick = {},
                modifier = Modifier.layoutId(Console.ID_BTN_SLEEP_TIMER)
            )

            IconButton(
                icon = Icons.Outlined.MoreHoriz,
                contentDescription = null,
                onClick = {},
                modifier = Modifier.layoutId(Console.ID_BTN_MORE)
            )
        }
    )
}