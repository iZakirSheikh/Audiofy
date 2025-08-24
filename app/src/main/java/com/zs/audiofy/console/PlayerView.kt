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

import android.app.Activity
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.ScreenLockRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.twotone.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ExperimentalMotionApi
import com.zs.audiofy.R
import com.zs.audiofy.common.WindowStyle
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LocalSystemFacade
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
    // BackHandler
    val navController = LocalNavController.current
    val facade = LocalSystemFacade.current
    val onNavigatingBack: () -> Unit = {
        navController.navigateUp()
    }
    BackHandler(onBack = onNavigatingBack)
    // Content
    val onColor = if (isVideo) Color.SignalWhite else AppTheme.colors.onBackground
    val visibility = viewState.visibility
    val enabled = visibility == C.VISIBILITY_VISIBLE
    CompositionLocalProvider(LocalContentColor provides onColor) {
        // Update constraints
        val clazz = LocalWindowSize.current
        val dpInsets = insets.toDpRect
        val constraints = remember(clazz, dpInsets, isVideo, visibility) {
            // Determine if controls are locked (cannot be shown/hidden by user)
            val isLocked = visibility == C.VISIBILITY_INVISIBLE_LOCKED

            // Update controller visibility based on media state and lock status
            when {
                !isVideo -> viewState.emit(C.VISIBILITY_VISIBLE)
                isVideo && !state.playing && !isLocked -> viewState.emit(C.VISIBILITY_VISIBLE)
                visibility == C.VISIBILITY_VISIBLE_LOCK -> viewState.emit(C.VISIBILITY_INVISIBLE_LOCKED, true)
                !isLocked -> viewState.emit(C.VISIBILITY_INVISIBLE, true)
            }

            // Update system bar style (auto vs hidden) depending on controller visibility
            facade.style = facade.style + when (visibility) {
                C.VISIBILITY_VISIBLE -> WindowStyle.FLAG_SYSTEM_BARS_VISIBILITY_AUTO
                else -> WindowStyle.FLAG_SYSTEM_BARS_HIDDEN
            }
            // Determine which controls to hide
            val excluded = when (visibility) {
                C.VISIBILITY_VISIBLE -> if (!isVideo) null else null // No exclusions when visible
                C.VISIBILITY_INVISIBLE, C.VISIBILITY_INVISIBLE_LOCKED -> emptyArray() // Hide everything
                C.VISIBILITY_VISIBLE_LOCK -> arrayOf(C.ID_BTN_LOCK) // Show only lock button
                C.VISIBILITY_VISIBLE_SEEK -> arrayOf(C.ID_SEEK_BAR, C.ID_EXTRA_INFO) // Show seek + info
                else -> error("Invalid visibility $visibility provided.") // Defensive check
            }
            // Compute the new ConstraintSet for layout adjustments
            calculateConstraintSet(clazz, dpInsets, isVideo, excluded)
        }
        ConstraintLayout(
            constraints.constraints,
            modifier = modifier,
            animateChangesSpec = tween(),
            content = {
                // Background
                Crossfade(
                    targetState = isVideo,
                    modifier = Modifier
                        .layoutId(C.ID_BACKGROUND)
                        .thenIf(isVideo && state.playing) {
                            clickable(
                                indication = null,
                                interactionSource = remember(::MutableInteractionSource),
                                onClick = {
                                    viewState.emit(
                                        newVisibility = when (visibility) {
                                            C.VISIBILITY_VISIBLE -> C.VISIBILITY_INVISIBLE
                                            C.VISIBILITY_INVISIBLE -> C.VISIBILITY_VISIBLE
                                            C.VISIBILITY_INVISIBLE_LOCKED -> C.VISIBILITY_VISIBLE_LOCK
                                            else -> C.VISIBILITY_INVISIBLE_LOCKED
                                        }
                                    )
                                }
                            )
                        },
                    content = { value ->
                        when (value) {
                            false -> Spacer(
                                Modifier
                                    .background(AppTheme.colors.background)
                                    .fillMaxSize()
                            )

                            else -> Spacer(
                                Modifier
                                    .background(Color.Black)
                                    .fillMaxSize()
                            )
                        }

                    }
                )

                // Video
                if (isVideo) {
                    val provider = viewState.provider
                    var scale by remember { mutableStateOf(ContentScale.Fit) }
                    VideoSurface(
                        provider = provider,
                        keepScreenOn = state.playWhenReady,
                        modifier = Modifier
                            .key(C.ID_VIDEO_SURFACE)
                            .resize(scale, state.videoSize)
                    )
                    // Scrim
                    Spacer(
                        modifier = Modifier
                            .layoutId(C.ID_SCRIM)
                            .background(Color.Black.copy(0.4f))
                    )
                    // Resize Mode
                    IconButton(
                        icon = if (scale == ContentScale.Fit) Icons.Outlined.Fullscreen else Icons.Outlined.FitScreen,
                        contentDescription = null,
                        onClick = {
                            scale =
                                if (scale == ContentScale.Fit) ContentScale.Crop else ContentScale.Fit
                        },
                        modifier = Modifier.layoutId(C.ID_BTN_RESIZE_MODE),
                        enabled = enabled
                    )
                    // Lock
                    val isLocked = visibility == C.VISIBILITY_INVISIBLE_LOCKED
                    IconButton(
                        icon = if (isLocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        contentDescription = null,
                        onClick = { viewState.emit(if (visibility == C.VISIBILITY_VISIBLE_LOCK) C.VISIBILITY_VISIBLE else C.VISIBILITY_VISIBLE_LOCK) },
                        modifier = Modifier.layoutId(C.ID_BTN_LOCK),
                        enabled = visibility >= C.VISIBILITY_VISIBLE_LOCK
                    )
                }

                // Collapse
                val accent = if (isVideo) onColor else AppTheme.colors.accent
                IconButton(
                    icon = Icons.Outlined.ExpandMore,
                    onClick = onNavigatingBack,
                    tint = accent,
                    enabled = enabled,
                    contentDescription = null,
                    modifier = Modifier
                        .key(C.ID_BTN_COLLAPSE)
                        .thenIf(!isVideo) {
                            border(AppTheme.colors.shine, CircleShape)
                                .background(AppTheme.colors.background(3.dp), shape = CircleShape)
                        },
                )

                // Playing bars.
                Icon(
                    painter = lottieAnimationPainter(
                        R.raw.playback_indicator,
                        isPlaying = state.playing
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = ContentPadding.small)
                        .lottie()
                        .key(C.ID_PLAYING_INDICATOR),
                    tint = accent
                )

                // Title
                Box(
                    modifier = Modifier
                        .key(C.ID_TITLE)
                        .clipToBounds(),
                    content = {
                        Label(
                            text = state.title ?: stringResource(id = R.string.unknown),
                            fontSize = constraints.titleTextSize.sp,// Maybe Animate
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

                // Slider
                TimeBar(
                    progress = chronometer.progress(state.duration),
                    onValueChange = {
                        if (isVideo)
                            viewState.emit(C.VISIBILITY_VISIBLE_SEEK)
                        val mills = (it * state.duration).toLong()
                        chronometer.raw = mills
                    },
                    onValueChangeFinished = {
                        if (isVideo)
                            viewState.emit(C.VISIBILITY_INVISIBLE)
                        val progress = chronometer.elapsed / state.duration.toFloat()
                        viewState.seekTo(progress)
                    },
                    modifier = Modifier.key(C.ID_SEEK_BAR),
                    enabled = state.duration > 0 && visibility >= C.VISIBILITY_VISIBLE_SEEK,
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
                    enabled = enabled && state.isNextAvailable, // add- logic
                    modifier = Modifier.key(C.ID_BTN_SKIP_TO_NEXT)
                )

                // Skip to Prev
                IconButton(
                    onClick = viewState::skipToPrev,
                    icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                    contentDescription = null,
                    enabled = enabled && state.isPrevAvailable,
                    modifier = Modifier.key(C.ID_BTN_SKIP_PREVIOUS)
                )

                // Repeat Mode
                IconButton(
                    onClick = viewState::cycleRepeatMode,
                    content = {
                        val mode = state.repeatMode
                        Icon(
                            painter = AnimVectorPainter(
                                R.drawable.avd_repeat_more_one_all,
                                mode == Remote.REPEAT_MODE_ALL
                            ),
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
                    onClick = (facade as Activity)::toggleRotationLock,
                    enabled = enabled,
                    modifier = Modifier.layoutId(C.ID_BTN_ROTATION_LOCK)
                )

                // Queue
                IconButton(
                    icon = Icons.Outlined.Queue,
                    contentDescription = null,
                    onClick = { },
                    enabled = enabled,
                    modifier = Modifier.layoutId(C.ID_BTN_QUEUE)
                )

                // Favourite
                LottieAnimatedButton(
                    R.raw.lt_twitter_heart_filled_unfilled,
                    onClick = viewState::toggleLike,
                    animationSpec = tween(800),
                    atEnd = state.favourite, // if fav
                    contentDescription = null,
                    progressRange = 0.13f..1.0f,
                    scale = 3.5f,
                    tint = accent,
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
        )
    }

    // Restore
    // This DisposableEffect manages the system UI appearance (light/dark bars)
    // based on whether the content is video or audio.
    DisposableEffect(isVideo) {
        // When isVideo changes, update the system bar appearance.
        // If it's a video, force dark system bars.
        // Otherwise, use the automatic system bar appearance.
        facade.style = when {
            isVideo -> facade.style + WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_DARK
            else -> facade.style + WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_AUTO
        }
        onDispose {
            // When the composable is disposed (e.g., navigating away),
            // revert the system bar appearance and visibility to their default automatic states.
            facade.style =
                facade.style + WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_AUTO + WindowStyle.FLAG_SYSTEM_BARS_VISIBILITY_AUTO
        }
    }
}