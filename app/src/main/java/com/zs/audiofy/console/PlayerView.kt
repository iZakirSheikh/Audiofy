@file:OptIn(ExperimentalSharedTransitionApi::class)

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

package com.zs.audiofy.console

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.ScreenLockRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.twotone.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.compose.shine
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.background
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import com.zs.audiofy.common.compose.rememberAnimatedVectorPainter as AnimVectorPainter

private const val TAG = "PlayerView"

/** Represents [NowPlaying] default state in [Console] */
private val NonePlaying = NowPlaying(null, null)

/** A short-hand   */
private fun Modifier.key(value: String) = layoutId(value).sharedElement(value)

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
    insets: WindowInsets,
    onNewAction: (code: Int) -> Boolean,
    modifier: Modifier = Modifier
) {
    // collect the state.
    val s by viewState.state.collectAsState()
    val state = s ?: NonePlaying
    // Calculate background and onColor
    val isVideo = state.isVideo
    val background = if (isVideo) Console.STYLE_BG_BLACK else Console.STYLE_BG_SIMPLE
    val onColor = when (background) {
        Console.STYLE_BG_SIMPLE, Console.STYLE_BG_AMBIENT -> AppTheme.colors.onBackground
        Console.STYLE_BG_BLACK -> Color.SignalWhite
        else -> AppTheme.colors.onBackground
    }
    val navController = LocalNavController.current
    val onNavigateBack:() -> Unit  =  {
        val handled = onNewAction(Console.ACTION_BACK_PRESS)
        if (!handled)
            navController.navigateUp()
    }
    BackHandler(onBack = onNavigateBack)

    // calculate the new constraints
    val clazz = LocalWindowSize.current
    val ldr = LocalLayoutDirection.current
    val density = LocalDensity.current
    val only by remember { mutableStateOf(emptyArray<String>()) }
    val constraints = remember(clazz, insets, isVideo, only) {
        val dpInsets = with(density) {
            // Convert raw insets to dp values, considering layout direction
            DpRect(
                left = insets.getLeft(density, ldr).toDp(),
                right = insets.getRight(this, ldr).toDp(),
                top = insets.getTop(this).toDp(),
                bottom = insets.getBottom(this).toDp()
            )
        }
        calculateConstraintSet(clazz, dpInsets, isVideo, only)
    }
    // Propagate onColor - Maybe add animation.
    CompositionLocalProvider(LocalContentColor provides onColor) {
        // Build The layout
        ConstraintLayout(
            constraintSet = constraints.value,
            modifier = modifier,
            animateChangesSpec = tween(),
            content = {
                // Background - Maybe add Crossfade effect.
                Spacer(
                    modifier = Modifier
                        .layoutId(Console.ID_BACKGROUND)
                        .background(AppTheme.colors.background(1.dp))
                        .visualEffect(ImageBrush.NoiseBrush)
                )

                // Video Placeholder.
                if (isVideo) {
                    Spacer(
                        modifier = Modifier
                            .key(Console.ID_VIDEO_SURFACE)
                            .background(Color.Black)
                    )
                    // Scrim
                    Spacer(
                        modifier = Modifier.layoutId(Console.ID_SCRIM).background(Color.Black.copy(0.3f))
                    )
                }

                // Collapse
                IconButton(
                    icon = Icons.Outlined.ExpandMore,
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .key(Console.ID_BTN_COLLAPSE)
                        .border(AppTheme.colors.shine, CircleShape)
                        .background(AppTheme.colors.background(3.dp), shape = CircleShape),
                    tint = AppTheme.colors.accent,
                    contentDescription = null,
                )

                // Playing bars.
                Icon(
                    painter = lottieAnimationPainter(R.raw.playback_indicator, isPlaying = state.playing),
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = ContentPadding.small).lottie().key(Console.ID_PLAYING_INDICATOR),
                    tint = AppTheme.colors.accent
                )

                // Title
                Box(
                    modifier = Modifier.key(Console.ID_TITLE).clipToBounds(),
                    content = {
                        Label(
                            text = state.title ?: stringResource(id = R.string.unknown),
                            fontSize = constraints.titleTextSize,// Maybe Animate
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.marque(Int.MAX_VALUE)
                        )
                    }
                )

                // Subtitle
                Label(
                    text = state.subtitle ?: "",
                    style = AppTheme.typography.label3,
                    modifier = Modifier.key(Console.ID_SUBTITLE),
                    color = onColor.copy(ContentAlpha.medium)
                )

                // Extra-info
                val chronometer = state.chronometer
                Label(
                    style = AppTheme.typography.label3,
                    color = onColor.copy(ContentAlpha.medium),
                    modifier = Modifier.key(Console.ID_POSITION),
                    text = buildString {
                        val elapsed = chronometer.elapsed
                        val fPos =
                            if (elapsed == Long.MIN_VALUE) "N/A" else DateUtils.formatElapsedTime(elapsed / 1000)
                        val duration = state.duration
                        val fDuration =
                            if (duration == Remote.TIME_UNSET) "N/A" else DateUtils.formatElapsedTime(duration / 1000)
                        append("$fPos / $fDuration (${stringResource(R.string.postfix_x_f, state.speed)})")
                    }
                )

                // Slider
                TimeBar(
                    progress = chronometer.progress(state.duration),
                    onValueChange = {
                        val mills = (it * state.duration).toLong()
                        chronometer.raw = mills
                    },
                    onValueChangeFinished = {
                        val progress = chronometer.elapsed / state.duration.toFloat()
                        viewState.seekTo(progress)
                    },
                    modifier = Modifier.key(Console.ID_SEEK_BAR),
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
                    tint = if (state.shuffle) AppTheme.colors.accent else onColor.copy(ContentAlpha.disabled),
                    modifier = Modifier.key(Console.ID_SHUFFLE)
                )

                // Skip to next
                IconButton(
                    onClick = viewState::skipToNext,
                    icon = Icons.Outlined.KeyboardDoubleArrowRight,
                    contentDescription = null,
                    enabled = true, // add- logic
                    modifier = Modifier.key(Console.ID_BTN_SKIP_TO_NEXT)
                )

                // Skip to Prev
                IconButton(
                    onClick = viewState::skipToPrev,
                    icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                    contentDescription = null,
                    enabled = true,
                    modifier = Modifier.key(Console.ID_BTN_SKIP_PREVIOUS)
                )

                // Repeat Mode
                IconButton(
                    onClick = viewState::cycleRepeatMode,
                    content = {
                        val mode = state.repeatMode
                        Icon(
                            painter = AnimVectorPainter(R.drawable.avd_repeat_more_one_all, mode == Remote.REPEAT_MODE_ALL),
                            contentDescription = null,
                            tint = onColor.copy(if (mode == Remote.REPEAT_MODE_OFF) ContentAlpha.disabled else ContentAlpha.high)
                        )
                    },
                    modifier = Modifier.key(Console.ID_BTN_REPEAT_MODE)
                )

                // Play Button
                PlayButton(
                    onClick = viewState::togglePlay,
                    isPlaying = state.playing,
                    style = if (isVideo) Console.STYLE_PLAY_BUTTON_SIMPLE else Console.STYLE_PLAY_OUTLINED,
                    modifier = Modifier.key(Console.ID_BTN_PLAY_PAUSE)
                )

                // Rotation
                IconButton(
                    icon = Icons.Outlined.ScreenLockRotation,
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

                // Favourite
                IconButton(
                    icon = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    onClick = {},
                    modifier = Modifier.layoutId(Console.ID_BTN_LIKED)
                )

                // Speed
                IconButton(
                    icon = Icons.Outlined.Speed,
                    contentDescription = null,
                    onClick = {},
                    modifier = Modifier.layoutId(Console.ID_BTN_PLAYBACK_SPEED)
                )

                // Timer
                IconButton(
                    icon = Icons.Outlined.Timer,
                    contentDescription = null,
                    onClick = {},
                    modifier = Modifier.layoutId(Console.ID_BTN_SLEEP_TIMER)
                )

                // Equalizer
                IconButton(
                    icon = Icons.Outlined.Tune,
                    contentDescription = null,
                    onClick = {},
                    modifier = Modifier.layoutId(Console.ID_BTN_EQUALIZER)
                )

                // Info
                IconButton(
                    icon = Icons.TwoTone.Info,
                    contentDescription = null,
                    onClick = {},
                    modifier = Modifier.layoutId(Console.ID_BTN_MEDIA_INFO)
                )

                // More
                IconButton(
                    icon = Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    onClick = {},
                    modifier = Modifier.layoutId(Console.ID_BTN_MORE)
                )

                if (!state.isVideo) {
                    Artwork(
                        model = state.artwork,
                        modifier = Modifier.key(Console.ID_ARTWORK),
                        border = 4.dp,
                        shape = AppTheme.shapes.xLarge,
                        shadow = 12.dp
                    )
                    return@ConstraintLayout
                }

                // Resize Mode
                IconButton(
                    icon = Icons.Outlined.Fullscreen,
                    contentDescription = null,
                    onClick = {},
                    modifier = Modifier.layoutId(Console.ID_BTN_RESIZE_MODE)
                )
            }
        )
    }
}