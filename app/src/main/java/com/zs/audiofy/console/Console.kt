/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
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

import android.util.Log
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpRect
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.Surface
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.core.playback.NowPlaying

// Represents some of the actions emitted by the widget.
// These are in float, allowing us to utilize positive values (0 to 1)
// to represent progress, while negative values are used for standard actions.
private const val EVENT_SHOW_NONE = -1f
private const val EVENT_SHOW_PROPERTIES = -2f
private const val EVENT_SHOW_SPEED_DIALOG = -3f
private const val EVENT_SHOW_SLEEP_DIALOG = -4f

private const val TAG = "Console"

// ACTIONS


/**
 * Extension property for [WindowInsets] that provides a [DpRect] representation of the insets,
 * ensuring layout compatibility across different screen densities and layout directions.
 *
 * @return A [DpRect] containing the left, top, right, and bottom insets in density-independent pixels (dp).
 */
private val WindowInsets.asDpRect: DpRect
    @Composable
    @ReadOnlyComposable
    get() {
        val ld =
            LocalLayoutDirection.current  // Get current layout direction for correct inset handling
        val density = LocalDensity.current    // Get current screen density for conversion to dp
        with(density) {
            // Convert raw insets to dp values, considering layout direction
            return DpRect(
                left = getLeft(density, ld).toDp(),
                right = getRight(this, ld).toDp(),
                top = getTop(this).toDp(),
                bottom = getBottom(this).toDp()
            )
        }
    }

@Composable
fun Console(viewState: ConsoleViewState) {
    val clazz = LocalWindowSize.current
    val secondary by remember { mutableFloatStateOf(EVENT_SHOW_NONE) }
    val insets = WindowInsets.systemBars.asDpRect
    val constrants = remember { Constraints(clazz, insets, false, false) }
    Log.d(TAG, "Console: console")
    Player(
        viewState,
        constrants,
        { false },
        Modifier
            .sharedBounds(RouteConsole.SHARED_ELEMENT_BACKGROUND)
            .fillMaxSize()
    )
}