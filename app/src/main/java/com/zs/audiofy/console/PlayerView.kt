@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMotionApi::class)

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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitScreen
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionLayoutScope
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.VideoSurface
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.compose.resize
import com.zs.audiofy.common.compose.shine
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.thenIf
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.zs.audiofy.common.compose.rememberAnimatedVectorPainter as AnimVectorPainter
import com.zs.audiofy.console.RouteConsole as C

private const val TAG = "PlayerView"

/** Represents [NowPlaying] default state in [Console] */
private val NonePlaying = NowPlaying(null, null)

/** A short-hand   */
private fun Modifier.key(value: String) = layoutId(value).sharedElement(value)

/**
 * Displays the [PlayerView] within the console interface.
 *
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
    val _state by viewState.state.collectAsState()
    val state = _state ?: NonePlaying
    val isVideo = state.isVideo
    // Content
    val onColor = if (isVideo) Color.SignalWhite else AppTheme.colors.onBackground
    var titleTextSize by remember { mutableIntStateOf(0) }
    var visibility by remember { mutableStateOf(C.CONTROLS_VISIBLE_ALL) }
    val enabled = visibility !== C.CONTROLS_VISIBLE_NONE
    val content: @Composable MotionLayoutScope.() -> Unit = content@{
        // Background
        Crossfade(
            isVideo,
            Modifier.layoutId(C.ID_BACKGROUND).thenIf(isVideo){
                clickable(indication = null, interactionSource = remember(::MutableInteractionSource) ){
                    visibility = if (visibility === C.CONTROLS_VISIBLE_ALL) C.CONTROLS_VISIBLE_NONE else C.CONTROLS_VISIBLE_ALL
                }
            },
            content = { value ->
                when (value) {
                    false -> Spacer(
                        Modifier.background(AppTheme.colors.background).fillMaxSize()
                    )
                    else -> Spacer(
                        Modifier.background(Color.Black).fillMaxSize()
                    )
                }

            }
        )

        // Video
        if (isVideo){
            val provider = viewState.provider
            val scale by mutableStateOf(ContentScale.Fit)
            VideoSurface(
                provider = provider,
                keepScreenOn = state.playWhenReady,
                modifier = Modifier
                    .key(C.ID_VIDEO_SURFACE)
                    .resize(scale, state.videoSize)
            )

            // Scrim
            Spacer(
                modifier = Modifier.layoutId(C.ID_SCRIM).background(Color.Black.copy(0.3f))
            )
            // Resize Mode
            IconButton(
                icon = if (scale == ContentScale.Fit) Icons.Outlined.Fullscreen else Icons.Outlined.FitScreen,
                contentDescription = null,
                onClick = { if (scale == ContentScale.Fit) ContentScale.Crop else ContentScale.Fit},
                modifier = Modifier.layoutId(C.ID_BTN_RESIZE_MODE),
                enabled = enabled
            )
        }

        // Collapse
        val accent = if (isVideo) onColor else AppTheme.colors.accent
        IconButton(
            icon = Icons.Outlined.ExpandMore,
            onClick = {},
            modifier = Modifier
                .key(C.ID_BTN_COLLAPSE)
                .border(AppTheme.colors.shine, CircleShape)
                .thenIf(!isVideo){background(AppTheme.colors.background(3.dp), shape = CircleShape)},
            tint = accent,
            enabled = enabled,
            contentDescription = null,
        )

        // Playing bars.
        Icon(
            painter = lottieAnimationPainter(R.raw.playback_indicator, isPlaying = state.playing),
            contentDescription = null,
            modifier = Modifier.padding(horizontal = ContentPadding.small).lottie().key(C.ID_PLAYING_INDICATOR),
            tint = accent
        )

        // Title
        Box(
            modifier = Modifier.key(C.ID_TITLE).clipToBounds(),
            content = {
                Label(
                    text = state.title ?: stringResource(id = R.string.unknown),
                    fontSize = titleTextSize.sp,// Maybe Animate
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.marque(Int.MAX_VALUE)
                )
            }
        )

        // Subtitle
        Label(
            text = state.subtitle ?: "",
            style = AppTheme.typography.label3,
            modifier = Modifier.key(C.ID_SUBTITLE),
            color = onColor.copy(ContentAlpha.medium)
        )

        // Extra-info
        val chronometer = state.chronometer
        Label(
            style = AppTheme.typography.label3,
            color = onColor.copy(ContentAlpha.medium),
            modifier = Modifier.key(C.ID_EXTRA_INFO),
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
                if (isVideo && visibility !== C.CONTROLS_VISIBLE_SEEK)
                    visibility = C.CONTROLS_VISIBLE_SEEK
                val mills = (it * state.duration).toLong()
                chronometer.raw = mills
            },
            onValueChangeFinished = {
                if (isVideo && visibility !== C.CONTROLS_VISIBLE_NONE)
                    visibility = C.CONTROLS_VISIBLE_NONE
                val progress = chronometer.elapsed / state.duration.toFloat()
                viewState.seekTo(progress)
            },
            modifier = Modifier.key(C.ID_SEEK_BAR),
            enabled = state.duration > 0 && enabled,
            accent = accent
        )

        // Shuffle
        LottieAnimatedButton(
            id = R.raw.lt_shuffle_on_off,
            onClick = { viewState.shuffle(!state.shuffle) },
            atEnd = state.shuffle,
            progressRange = 0f..0.8f,
            scale = 1.5f,
            contentDescription = null,
            tint = if (state.shuffle) accent else onColor.copy(ContentAlpha.disabled),
            modifier = Modifier.key(C.ID_SHUFFLE),
            enabled = enabled
        )

        // Skip to next
        IconButton(
            onClick = viewState::skipToNext,
            icon = Icons.Outlined.KeyboardDoubleArrowRight,
            contentDescription = null,
            enabled = enabled, // add- logic
            modifier = Modifier.key(C.ID_BTN_SKIP_TO_NEXT)
        )

        // Skip to Prev
        IconButton(
            onClick = viewState::skipToPrev,
            icon = Icons.Outlined.KeyboardDoubleArrowLeft,
            contentDescription = null,
            enabled = enabled,
            modifier = Modifier.key(C.ID_BTN_SKIP_PREVIOUS)
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
            modifier = Modifier.key(C.ID_BTN_REPEAT_MODE),
            enabled = enabled
        )

        // Play Button
        PlayButton(
            onClick = viewState::togglePlay,
            isPlaying = state.playing,
            simple = isVideo,
            enabled = enabled,
            modifier = Modifier.key(C.ID_BTN_PLAY_PAUSE)
        )

        // Rotation
        IconButton(
            icon = Icons.Outlined.ScreenLockRotation,
            contentDescription = null,
            onClick = { /*onNewAction(C.ACTION_TOGGLE_ROTATION_LOCK)*/ },
            enabled = enabled,
            modifier = Modifier.layoutId(C.ID_BTN_ROTATION_LOCK)
        )

        // Queue
        IconButton(
            icon = Icons.Outlined.Queue,
            contentDescription = null,
            onClick = {  },
            enabled = enabled,
            modifier = Modifier.layoutId(C.ID_BTN_QUEUE)
        )

        // Favourite
        IconButton(
            icon = Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            onClick = {},
            enabled = enabled,
            modifier = Modifier.layoutId(C.ID_BTN_LIKED)
        )

        // Speed
        IconButton(
            icon = Icons.Outlined.Speed,
            contentDescription = null,
            onClick = {},
            enabled = enabled,
            modifier = Modifier.layoutId(C.ID_BTN_PLAYBACK_SPEED)
        )

        // Timer
        IconButton(
            icon = Icons.Outlined.Timer,
            contentDescription = null,
            onClick = {},
            enabled = enabled,
            modifier = Modifier.layoutId(C.ID_BTN_SLEEP_TIMER)
        )

        // Equalizer
        IconButton(
            icon = Icons.Outlined.Tune,
            contentDescription = null,
            onClick = {},
            enabled = enabled,
            modifier = Modifier.layoutId(C.ID_BTN_EQUALIZER)
        )

        // Info
        IconButton(
            icon = Icons.TwoTone.Info,
            contentDescription = null,
            onClick = {},
            enabled = enabled,
            modifier = Modifier.layoutId(C.ID_BTN_MEDIA_INFO)
        )

        // More
        IconButton(
            icon = Icons.Outlined.MoreHoriz,
            contentDescription = null,
            onClick = {},
            enabled = enabled,
            modifier = Modifier.layoutId(C.ID_BTN_MORE)
        )

        if (!state.isVideo)
            Artwork(
                model = state.artwork,
                modifier = Modifier.key(C.ID_ARTWORK),
                border = 4.dp,
                shape = AppTheme.shapes.xLarge,
                shadow = 12.dp
            )
    }
    // layout
    CompositionLocalProvider(
        LocalContentColor provides onColor,
        content = {
            // Update constraints
            val clazz = LocalWindowSize.current
            val dpInsets = insets.toDpRect
            var start by remember {
                val start = calculateConstraintSet(clazz, dpInsets, isVideo, visibility)
                titleTextSize = start.titleTextSize
                mutableStateOf(start.constraints)
            }
            var end by remember { mutableStateOf(start) }
            val progress = remember { Animatable(0.0f) }
            var direction by remember { mutableIntStateOf(1) }
            LaunchedEffect(clazz, dpInsets, isVideo, visibility) {
                val newConstraints = calculateConstraintSet(clazz, dpInsets, isVideo, visibility)

                if (isVideo && (visibility !== C.CONTROLS_VISIBLE_NONE)) {
                    launch {
                        delay(30_00)
                        visibility = C.CONTROLS_VISIBLE_NONE
                    }
                }

                val currentConstraints = if (direction == 1) start else end
                if (newConstraints.constraints != currentConstraints) {
                    titleTextSize = newConstraints.titleTextSize
                    if (direction == 1) end = newConstraints.constraints else start =
                        newConstraints.constraints
                    progress.animateTo(direction.toFloat(), tween())
                    direction = if (direction == 1) 0 else 1
                }
            }
            MotionLayout(
                start = start,
                end = end,
                progress = progress.value,
                content = content,
                modifier = modifier
            )
        }
    )
}