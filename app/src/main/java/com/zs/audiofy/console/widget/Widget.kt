/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 30-06-2025.
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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import com.zs.audiofy.MainActivity
import com.zs.audiofy.common.IAP_COLOR_CROFT_GOLDEN_DUST
import com.zs.audiofy.common.IAP_COLOR_CROFT_GRADIENT_GROVES
import com.zs.audiofy.common.IAP_COLOR_CROFT_MISTY_DREAM
import com.zs.audiofy.common.IAP_COLOR_CROFT_ROTATING_GRADIENT
import com.zs.audiofy.common.IAP_COLOR_CROFT_WAVY_GRADIENT_DOTS
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_DISK_DYNAMO
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_ELONGATE_BEAT
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_IPHONE
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_SKEWED_DYNAMIC
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_SNOW_CONE
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_TIRAMISU
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.preference
import com.zs.audiofy.common.compose.scale
import com.zs.audiofy.common.domain
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.styles.DiskDynamo
import com.zs.audiofy.console.widget.styles.ElongatedBeat
import com.zs.audiofy.console.widget.styles.GoldenDust
import com.zs.audiofy.console.widget.styles.GradientGroves
import com.zs.audiofy.console.widget.styles.Iphone
import com.zs.audiofy.console.widget.styles.MistyDream
import com.zs.audiofy.console.widget.styles.MistyTunes
import com.zs.audiofy.console.widget.styles.RedVioletCake
import com.zs.audiofy.console.widget.styles.RotatingColorGradient
import com.zs.audiofy.console.widget.styles.SkewedDynamic
import com.zs.audiofy.console.widget.styles.SnowCone
import com.zs.audiofy.console.widget.styles.Tiramisu
import com.zs.audiofy.console.widget.styles.WavyGradientDots
import com.zs.audiofy.settings.AppConfig
import com.zs.audiofy.settings.Settings
import com.zs.compose.theme.LocalNavAnimatedVisibilityScope
import com.zs.core.billing.Paymaster
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.compose.foundation.combinedClickable as clickable

/** Represents the [Widget] of the console.*/
object Widget {
    /** Represents the max-width of the inApp Player widget.*/
    private val LimitedBy =
        Modifier
            .padding(start = 50.dp, end = ContentPadding.large)
            .widthIn(max = 400.dp)
    val FabSize = Modifier.requiredSize(112.dp, 56.dp)
    val FabVideoSize = Modifier.requiredSize(156.dp, 88.dp)

    // Represents some of the actions emitted by the widget.
    // These are in float, allowing us to utilize positive values (0 to 1)
    // to represent progress, while negative values are used for standard actions.
    const val REQUEST_PLAY_TOGGLE = -1f
    const val REQUEST_SKIP_TO_NEXT = -2f
    const val REQUEST_SKIP_TO_PREVIOUS = -3f
    const val REQUEST_LIKED = -4f
    const val REQUEST_OPEN_CONSOLE = -5f
    const val REQUEST_CLOSE = -6f
    const val REQUEST_SHOW_CONFIG = -7f

    // Represent the IDs of supported players.
    const val ID_FAB_PLAYER = "fab_player"
    const val ID_CONFIG = "config"
    const val ID_PLACEHOLDER = "_place_holder"
    const val ID_FAB_VIDEO_PLAYER = "fab_video_player"

    @Composable
    operator fun invoke(surface: HazeState, modifier: Modifier = Modifier) {
        val facade = LocalSystemFacade.current
        val remote = (facade as? MainActivity)?.relay ?: return Spacer(modifier)
        // Get the navigation controller.
        val navController = LocalNavController.current
        val s by remote.state.collectAsState()
        val state = s ?: return Spacer(modifier)
        val entry by navController.currentBackStackEntryAsState()
        val isConsole = entry?.destination?.domain == RouteConsole.domain
        // State to track whether the widget is expanded or not.
        var expanded by remember { mutableStateOf(false) }
        // Handler to collapse the widget when back is pressed.
        var showConfigScreen by remember { mutableStateOf(false) }
        val onDismissRequest = { if (showConfigScreen) showConfigScreen = false else expanded = false }
        BackHandler(expanded && !isConsole, onDismissRequest)
        // Action handler
        val scope = rememberCoroutineScope()
        val onRequest: (Float) -> Unit = { code: Float ->
            scope.launch {
                when (code) {
                    REQUEST_PLAY_TOGGLE -> remote.togglePlay()
                    REQUEST_SKIP_TO_NEXT -> remote.skipToNext()
                    REQUEST_LIKED -> remote.toggleLike()
                    REQUEST_CLOSE -> remote.clear()
                    REQUEST_SKIP_TO_PREVIOUS -> remote.skipToPrevious()
                    REQUEST_OPEN_CONSOLE -> navController.navigate(RouteConsole())
                    REQUEST_SHOW_CONFIG -> showConfigScreen = !showConfigScreen // toggle config.
                    else -> remote.seekTo(code)
                }
            }
        }
        // Modifier for clickable behavior.
        val clickable = Modifier.clickable(
            // No ripple effect.
            interactionSource = null,
            // Scale animation on click.
            indication = scale(),
            // Toggle expanded state on click.
            onClick = {
                val isFab = !expanded
                // Determine if the player is currently in FAB (mini) mode.
                // If not expanded, it's considered a FAB player.
                when {
                    state.isVideo ->
                        onRequest(REQUEST_PLAY_TOGGLE)
                    // If currently viewing a video, toggle playback (play/pause).

                    isFab && AppConfig.fabLongPressLaunchConsole ->
                        expanded = true
                    // If in FAB mode and the "long-press FAB opens console" setting is enabled,
                    // expand the player to show the console.

                    isFab && !AppConfig.fabLongPressLaunchConsole ->
                        navController.navigate(RouteConsole())
                    // If in FAB mode but the "long-press FAB opens console" setting is disabled,
                    // navigate directly to the console screen without expanding.
                    else -> expanded = false
                }
            },
            // Navigate to console on long click if not expanded, otherwise collapse.
            onLongClick = {
                val isFab = !expanded
                // Determine if the player is currently in FAB (mini) mode.
                // If not expanded, it's considered a FAB player.

                when {
                    isFab && AppConfig.fabLongPressLaunchConsole ->
                        // If in FAB mode AND the user preference "long-press FAB opens console" is enabled,
                        // navigate to the console screen.
                        navController.navigate(RouteConsole())

                    isFab && !AppConfig.fabLongPressLaunchConsole ->
                        // If in FAB mode but the preference is disabled,
                        // expand the player to show the full-screen view instead.
                        expanded = true

                    else ->
                        // If the player is already expanded (not in FAB mode),
                        // collapse it back to FAB mode.
                        expanded = false
                }
            }
        )
        // Layout
        val widget by preference(Settings.GLANCE)
        AnimatedContent(
            // choose target appropriately.
            targetState = when {
                isConsole -> ID_PLACEHOLDER
                showConfigScreen -> ID_CONFIG
                state.isVideo -> ID_FAB_VIDEO_PLAYER
                !expanded -> ID_FAB_PLAYER
                else -> widget
            },
            modifier = LimitedBy then modifier,
            content = { id ->
                // Provide the current scope for navigation animations.
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    when (id) {
                        ID_CONFIG -> Config(state, surface, onRequest)
                        ID_PLACEHOLDER -> Spacer(FabSize)
                        ID_FAB_PLAYER -> FabPlayer(state, onRequest, clickable)
                        ID_FAB_VIDEO_PLAYER -> FabVideoPlayer(state, runBlocking { remote.getViewProvider() }, onRequest, clickable)
                        Paymaster.IAP_PLATFORM_WIDGET_DISK_DYNAMO -> DiskDynamo(state, onRequest, clickable)
                        Paymaster.IAP_PLATFORM_WIDGET_ELONGATE_BEAT -> ElongatedBeat(state, onRequest, clickable)
                        Paymaster.IAP_COLOR_CROFT_GOLDEN_DUST -> GoldenDust(state, onRequest, clickable)
                        Paymaster.IAP_COLOR_CROFT_GRADIENT_GROVES -> GradientGroves(state, onRequest, clickable)
                        Paymaster.IAP_PLATFORM_WIDGET_IPHONE -> MistyTunes(state, surface, onRequest, clickable)
                        Paymaster.IAP_COLOR_CROFT_MISTY_DREAM -> MistyDream(state, onRequest, clickable)
                        Paymaster.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE -> RedVioletCake(state, onRequest, clickable)
                        Paymaster.IAP_COLOR_CROFT_ROTATING_GRADIENT -> RotatingColorGradient(state, onRequest, clickable)
                        Paymaster.IAP_PLATFORM_WIDGET_SKEWED_DYNAMIC -> SkewedDynamic(state, onRequest, clickable)
                        Paymaster.IAP_PLATFORM_WIDGET_SNOW_CONE -> SnowCone(state, onRequest, clickable)
                        Paymaster.IAP_PLATFORM_WIDGET_TIRAMISU -> Tiramisu(state, onRequest, clickable)
                        Paymaster.IAP_COLOR_CROFT_WAVY_GRADIENT_DOTS -> WavyGradientDots(state, onRequest, clickable)
                        else -> Iphone(state,  onRequest, clickable)
                    }
                }
            }
        )
    }
}