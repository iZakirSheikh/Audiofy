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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.MainActivity
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.scale
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.common.domain
import com.zs.audiofy.console.Console
import com.zs.audiofy.console.RouteConsole
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.foreground
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalNavAnimatedVisibilityScope
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.core.playback.NowPlaying
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable as clickable

private const val TAG = "MiniPlayer"

//  The size of the small pill shaped fab.
//  TODO - May add other values.
private val FAB_PLAYER_SIZE = DpSize(112.dp, 56.dp)

/** Represents the [MiniPlayer] in the console.*/
@Composable
private fun FabPlayer(
    state: NowPlaying,
    onAction: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .sharedBounds(Console.ID_BACKGROUND)
            .border(AppTheme.colors.shine, AppTheme.shapes.large)
            .shadow(8.dp, AppTheme.shapes.large)
            .background(AppTheme.colors.background(1.dp))
            .requiredSize(FAB_PLAYER_SIZE),
        content = {
            // Artwork
            AsyncImage(
                state.artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .sharedElement(Console.ID_ARTWORK)
                    .matchParentSize()
                    .clip(AppTheme.shapes.large)
                    .foreground(Color.Black.copy(0.5f), shape = AppTheme.shapes.large)
            )

            // Playing bars.
            Icon(
                painter = lottieAnimationPainter(R.raw.playback_indicator, isPlaying = state.playing),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = ContentPadding.medium)
                    .sharedElement("playback_indicator")
                    .lottie()
                    .align(Alignment.CenterStart),
                tint = Color.SignalWhite
            )

            // Play Toggle
            IconButton(
                onClick = { onAction(MiniPlayer.ACTION_PLAY_TOGGLE) },
                modifier = Modifier
                    .sharedElement(Console.ID_BTN_PLAY_PAUSE)
                    .align(Alignment.CenterEnd),
                content = {
                    Icon(
                        painter = lottieAnimationPainter(
                            id = R.raw.lt_play_pause,
                            atEnd = state.playing,
                            progressRange = 0.0f..0.29f,
                            animationSpec = tween(easing = LinearEasing)
                        ),
                        contentDescription = null,
                        tint = Color.SignalWhite,
                        modifier = Modifier.lottie(1.5f)
                    )
                },
            )
        }
    )
}

/** Represents the [MiniPlayer] in the console.*/
object MiniPlayer {
    /** Represents the max-width of the inApp Player widget.*/
    val WIDGET_MAX_WIDTH = 400.dp

    //  Represents the margin around widget.
    val START_PADDING = 50.dp
    val END_PADDING = ContentPadding.large

    // Represents the id of the the fabPlayer
    const val FAB_PLAYER_ID = "fab_player"
    const val HAZE_PLAYER_ID = "haze_player"
    const val EMPTY_PLAYER_ID = "empty_player"
    // Represents some of the actions emitted by the widget.
    // These are in float, allowing us to utilize positive values (0 to 1)
    // to represent progress, while negative values are used for standard actions.
    const val ACTION_PLAY_TOGGLE = -1f
    const val ACTION_SKIP_TO_NEXT = -2f
    const val ACTION_SKIP_TO_PREVIOUS = -3f
    const val ACTION_ADD_TO_LIKED = -4f
    const val ACTION_OPEN_CONSOLE = -5f

    @Composable
    @NonRestartableComposable
    operator fun invoke(surface: HazeState, modifier: Modifier = Modifier) {
        val facade = LocalSystemFacade.current
        val remote = (facade as? MainActivity)?.relay ?: return Spacer(modifier)
        // Get the navigation controller.
        val navController = LocalNavController.current
        val state by remote.state.collectAsState()
        val isConsole = navController.currentDestination?.domain == RouteConsole.domain
        // State to track whether the widget is expanded or not.
        var expanded by remember { mutableStateOf(false) }
        // Handler to collapse the widget when back is pressed.
        val onDismissRequest = { expanded = false }
        BackHandler(expanded && !isConsole) { onDismissRequest() }
        // Action handler
        val scope = rememberCoroutineScope()
        val onPlayerAction: (Float) -> Unit = { value: Float ->
            scope.launch {
                when (value) {
                    ACTION_PLAY_TOGGLE -> remote.togglePlay()
                    ACTION_SKIP_TO_NEXT -> remote.skipToNext()
                    ACTION_SKIP_TO_PREVIOUS -> remote.skipToPrevious()
                    ACTION_OPEN_CONSOLE -> navController.navigate(RouteConsole())
                    else -> remote.seekTo(value)
                }
            }
        }
        // Modifier for clickable behavior.
        val clickable = Modifier
            .clickable(
                // No ripple effect.
                interactionSource = null,
                // Scale animation on click.
                indication = scale(),
                // Toggle expanded state on click.
                onClick = { expanded = !expanded },
                // Navigate to console on long click if not expanded, otherwise collapse.
                onLongClick = {
                    if (!expanded)
                        navController.navigate(RouteConsole())
                    else expanded = false
                }
            )
        AnimatedContent(
            // choose target appropriately.
           targetState =  when {
                isConsole || state == null -> EMPTY_PLAYER_ID
                expanded -> HAZE_PLAYER_ID
                else -> FAB_PLAYER_ID
            },
            modifier = Modifier.padding(start = START_PADDING, end = END_PADDING).widthIn(max = WIDGET_MAX_WIDTH) then modifier,
            content = { value ->
                // Provide the current scope for navigation animations.
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    // Get the user's preference for the Glance widget.
                    // val widget by preference(Settings.GLANCE)
                    when (value) {
                        EMPTY_PLAYER_ID -> Spacer(Modifier.requiredSize(FAB_PLAYER_SIZE))
                        // Show mini player if not expanded.
                        HAZE_PLAYER_ID -> Hazy(state!!, surface, onPlayerAction, clickable)
                        // Show full screen Glance layout if expanded.
                        else -> FabPlayer(state!!, onPlayerAction, modifier = clickable)
                    }
                }
            }
        )
    }
}