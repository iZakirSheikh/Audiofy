/*
 * Copyright (c)  2026 Zakir Sheikh
 *
 * Created by sheik on 26 of Jan 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last Modified by sheik on 26 of Jan 2026
 *
 */

package com.prime.media.console

import android.annotation.SuppressLint
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import com.zs.core.playback.NowPlaying2
import android.app.Activity
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.Route
import com.prime.media.common.collectAsState
import com.prime.media.old.common.LocalNavController
import com.primex.core.SignalWhite
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.PlaybackController
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding as CP
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.Range
import com.zs.core_ui.WindowSize
import com.zs.core_ui.WindowStyle
import com.zs.core_ui.adaptive.HorizontalTwoPaneStrategy
import com.zs.core_ui.adaptive.SinglePaneStrategy
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.VerticalTwoPaneStrategy
import kotlinx.coroutines.delay

object RouteConsole: Route {
    // Component IDs
    const val ID_PLAYING_INDICATOR = "_playing_indicator"
    const val ID_BTN_COLLAPSE = "_btn_collapse"
    const val ID_ARTWORK = "_artwork"
    const val ID_TITLE = "_title"
    const val ID_SUBTITLE = "_subtitle"
    const val ID_EXTRA_INFO = "_extra_info"
    const val ID_SHUFFLE = "_shuffle"
    const val ID_BTN_REPEAT_MODE = "_btn_repeat_mode"
    const val ID_BTN_SKIP_PREVIOUS = "_btn_skip_previous"
    const val ID_BTN_PLAY_PAUSE = "_play_pause"
    const val ID_BTN_SKIP_TO_NEXT = "_skip_next"
    const val ID_SEEK_BAR = "_seek_bar"
    const val ID_VIDEO_SURFACE = "_video_surface"
    const val ID_BACKGROUND = "_background"
    const val ID_SCRIM = "_scrim"
    const val ID_BTN_RESIZE_MODE = "_resize_mode"
    const val ID_BTN_ROTATION_LOCK = "_rotation_lock"
    const val ID_BTN_QUEUE = "_queue"
    const val ID_BTN_SLEEP_TIMER = "_sleep_timer"
    const val ID_BTN_PLAYBACK_SPEED = "_playback_speed"
    const val ID_BTN_EQUALIZER = "_equalizer"
    const val ID_BTN_MEDIA_INFO = "_media_info"
    const val ID_BTN_LIKED = "_liked"
    const val ID_BTN_MORE = "_more"
    const val ID_BTN_LOCK = "_lock"
    const val ID_CAPTIONS = "_captions"
    const val ID_BANNER_AD = "_banner_ad"
    const val ID_CUES = "_cues"

    const val VISIBILITY_AUTO_HIDE_DELAY = 5_000L

    // Controller Visibility States
    const val VISIBLE_NONE = 0         // Fully hidden
    const val VISIBLE_NONE_LOCKED = 1  // Hidden + locked
    const val VISIBLE_LOCKED_SEEK = 3  // Locked, seek bar only (⚠️ duplicate of VISIBLE_SEEK)
    const val VISIBLE_LOCKED_LOCK = 4  // Locked, only lock icon shown
    const val VISIBLE_SEEK = 5         // Auto-hides, seek bar stays
    const val VISIBLE = 6              // Visible, auto-hides after timeout
    const val VISIBLE_ALWAYS = 7       // Always visible (default for audio)


    // Represents different dialogs to be shown
    private const val SHOW_NONE = 0
    private const val SHOW_TIMER = 1
    private const val SHOW_SPEED = 2
    private const val SHOW_MEDIA_INFO = 3
    private const val SHOW_MORE = 4
    private const val SHOW_MEDIA_CONFIG = 5


    private const val TAG = "Console"

    /** Represents [NowPlaying] default state in [Console] */
    private val NonePlaying = NowPlaying2(null, null)
    private val COLOR_BACKGROUND = Color(0xFF0E0E0F)

    /** A short-hand   */
    private fun Modifier.key(value: String) = layoutId(value)

    //    .thenIf(AppConfig.isWidgetToConsoleTransitionEnabled){ sharedElement(value) }
    private val DefaultAnimSpecs = tween<Float>()

    // Represents the shadow around the tef Cue.
    private val SCRIM_STYLE = Brush.verticalGradient(
        0f to Color.Black,                     // Top: solid black
        0.1f to Color.Black.copy(alpha = 0.5f),// Slightly lower: semi-transparent black
        0.2f to Color.Transparent,             // Transition to fully transparent
        0.8f to Color.Transparent,             // Stays transparent until near the bottom
        0.9f to Color.Black.copy(alpha = 0.5f),// Near bottom: semi-transparent black again
        1f to Color.Black                      // Bottom: solid black
    )

    // Represents the shapes of content/details in different configurations.
    private val PrimaryHorizontal = RoundedCornerShape(topEndPercent = 8, bottomEndPercent = 8)
    private val PrimaryVertical = RoundedCornerShape(bottomStartPercent = 8, bottomEndPercent = 8)
    private val SecondaryVertical = RoundedCornerShape(topStartPercent = 8, topEndPercent = 8)
    private val SecondaryHorizontal =
        RoundedCornerShape(topStartPercent = 8, bottomStartPercent = 8)
    private val ArtworkShape = RoundedCornerShape(12)

    // background styles
    const val BG_STYLE_AUTO = 0
    const val BG_STYLE_DARK = 1
    const val BG_STYLE_AUTO_ACRYLIC = 2
    const val BG_STYLE_DARK_ACRYLIC = 3

    // playbutton styles
    const val PLAY_BTN_STYLE_SIMPLE = 0
    const val PLAY_BTN_STYLE_OUTLINED = 1

    @SuppressLint("UnusedBoxWithConstraintsScope")
    @Composable
    @NonRestartableComposable
    operator fun invoke(viewState: ConsoleViewState) {
        // Describe content of primary pane.
        val facade = LocalSystemFacade.current
        val navController = LocalNavController.current

        var showViewOf by remember { mutableIntStateOf(SHOW_NONE) }
        var showQueue by remember { mutableStateOf(false) }
        val visibility = viewState.visibility

        // BackHandler
        val onNavigateBack = onBack@{
            if (showViewOf != SHOW_NONE || showQueue) {
                showViewOf = SHOW_NONE
                showQueue = false
                return@onBack
            }
            // Consume request if locked.
            if (viewState.visibility == VISIBLE_NONE_LOCKED) {
                viewState.emit(VISIBLE_LOCKED_LOCK, true)
                return@onBack
            }

            // Check if the activity has orientation lock enabled - unlock it.
            if ((facade as Activity).isOrientationLocked) {
                facade.toggleRotationLock()
                return@onBack
            }
            // restore system bars
            // When the composable is disposed (e.g., navigating away),
            // revert the system bar appearance and visibility to their default automatic states.
            facade.style =
                facade.style + WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_AUTO + WindowStyle.FLAG_SYSTEM_BARS_VISIBILITY_AUTO
            navController.navigateUp()
        }
        BackHandler(onBack = onNavigateBack)

        // Primary
        val state by viewState.state.collectAsState(default = NonePlaying)
        var titleTextSize by remember { mutableIntStateOf(16) }
        // TODO - Maybe allow users to set background using pref.
        val background = if (state.isVideo) BG_STYLE_DARK else BG_STYLE_AUTO_ACRYLIC
        val controller: @Composable () -> Unit = {

        }
        // Layout
        // Compute Two pane strategy
        val clazz = LocalWindowSize.current
        val insets = WindowInsets.systemBars
        val strategy = when {
            !showQueue || clazz.heightRange < Range.Medium && clazz.widthRange <= Range.Medium -> SinglePaneStrategy
            clazz.widthRange < clazz.heightRange -> VerticalTwoPaneStrategy(0.35f)
            else -> HorizontalTwoPaneStrategy(0.55f)
        }

        // Layout
        TwoPane(
            modifier = Modifier/*.thenIf(AppConfig.isWidgetToConsoleTransitionEnabled){ sharedBounds(ID_BACKGROUND)}*/,
            strategy = strategy,
            background = COLOR_BACKGROUND,
            spacing = CP.normal,
            secondary = {
                val insets = when {
                    strategy is VerticalTwoPaneStrategy -> insets.only(WindowInsetsSides.Bottom)
                    else -> insets.only(WindowInsetsSides.End + WindowInsetsSides.Top)
                }
                val shape = when (strategy) {
                    SinglePaneStrategy -> RectangleShape
                    is HorizontalTwoPaneStrategy -> SecondaryHorizontal
                    else -> SecondaryVertical
                }
                Queue(viewState, shape, insets)
            },
            primary = {
                BoxWithConstraints {
                    val insets = when {
                        strategy is VerticalTwoPaneStrategy -> insets.only(WindowInsetsSides.Top).toDpRect
                        else -> insets.toDpRect
                    }
                    // Compute constraints
                    val isVideo = state.isVideo
                    // compute key
                    var key = maxWidth.hashCode()
                    key = 31 * key + maxHeight.hashCode()
                    key = 31 * key + isVideo.hashCode()
                    key = 31 * key + state.state.hashCode()
                    key = 31 * key + insets.hashCode()
                    key = 31 * key + visibility.hashCode()
                    val constraints = remember(key) {
                        // Update system bar style (auto vs hidden) depending on controller visibility
                        facade.style += when (visibility) {
                            VISIBLE, VISIBLE_ALWAYS -> WindowStyle.FLAG_SYSTEM_BARS_VISIBILITY_AUTO
                            else -> WindowStyle.FLAG_SYSTEM_BARS_HIDDEN
                        }

                        // emit new value for visibilty.
                        when {
                            !isVideo && visibility != VISIBLE_ALWAYS -> viewState.emit(
                                VISIBLE_ALWAYS
                            )

                            isVideo && !state.playing && visibility != VISIBLE_ALWAYS -> viewState.emit(
                                VISIBLE_ALWAYS,
                                true
                            )

                            visibility == VISIBLE_LOCKED_LOCK -> viewState.emit(
                                VISIBLE_NONE_LOCKED,
                                true
                            )

                            visibility == VISIBLE_LOCKED_SEEK && state.state != PlaybackController.PLAYER_STATE_BUFFERING -> viewState.emit(
                                VISIBLE_LOCKED_LOCK,
                                true
                            )

                            visibility == VISIBLE -> viewState.emit(VISIBLE_NONE, true)
                        }

                        // Determine which controls to hide
                        val excluded = when (visibility) {
                            VISIBLE_ALWAYS, VISIBLE -> if (!isVideo) null else null // No exclusions when visible
                            VISIBLE_NONE, VISIBLE_NONE_LOCKED -> emptyArray() // Hide everything
                            VISIBLE_LOCKED_LOCK -> arrayOf(
                                ID_BTN_LOCK,
                                ID_SCRIM
                            ) // Show only lock button
                            VISIBLE_SEEK, VISIBLE_LOCKED_SEEK -> arrayOf(
                                ID_SEEK_BAR,
                                ID_EXTRA_INFO,
                                ID_SCRIM
                            ) // Show seek + info
                            else -> error("Invalid visibility $visibility provided.") // Defensive check
                        }
                        val clazz = WindowSize(DpSize(maxWidth, maxHeight))
                        // Compute the new ConstraintSet for layout adjustments
                        val c = calculateConstraintSet(clazz, insets, isVideo, excluded)
                        titleTextSize = c.titleTextSize
                        return@remember c
                    }
                    val onColor = when (background) {
                        BG_STYLE_DARK -> Color.SignalWhite
                        else -> AppTheme.colors.onBackground
                    }
                    CompositionLocalProvider(LocalContentColor provides onColor) {
                        ConstraintLayout(
                            constraints.constraints,
                            animateChangesSpec = DefaultAnimSpecs,
                            content = controller,
                            modifier = Modifier
                                .fillMaxSize()
                                .animateContentSize()
                                .then(
                                    when (strategy) {
                                        SinglePaneStrategy -> Modifier
                                        is HorizontalTwoPaneStrategy -> Modifier.clip(
                                            PrimaryHorizontal
                                        )

                                        else -> Modifier.clip(PrimaryVertical)
                                    }
                                ),
                        )
                    }
                }
            }
        )

        // Update system bars style.
        SideEffect {
            facade.style = when (background) {
                BG_STYLE_DARK -> facade.style + WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_DARK
                else -> facade.style + WindowStyle.FLAG_SYSTEM_BARS_APPEARANCE_AUTO
            }
        }
        //
        when (showViewOf) {
            SHOW_SPEED -> PlaybackSpeed(true, viewState.playbackSpeed) { newValue ->
                if (newValue == -1f) {
                    showViewOf = SHOW_NONE
                    return@PlaybackSpeed
                }
                val fValue =
                    (facade as Activity).getString(R.string.playback_speed_dialog_x_f, newValue)
                facade.showToast(fValue)
                viewState.playbackSpeed = newValue
            }

            SHOW_TIMER -> SleepTimer(true) { mills ->
                if (mills == -1L) {
                    showViewOf = SHOW_NONE
                    return@SleepTimer
                }
                viewState.sleepAt(mills)
                showViewOf = SHOW_NONE
            }
        }
    }
}
