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

package com.zs.audiofy.console

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.sharedBounds

/**
 * Represents container for Console screen; that hosts default constants, ids etc. and maybe in future its [Route]
 */
@OptIn(ExperimentalSharedTransitionApi::class)
object Console {
    private const val TAG = "Console"

    // Layout IDs
    const val ID_EQ_BARS = "_eq_bars"
    const val ID_BTN_COLLAPSE = "_btn_collapse"
    const val ID_ARTWORK = "_artwork"
    const val ID_TITLE = "_title"
    const val ID_SUBTITLE = "_subtitle"
    const val ID_POSITION = "_position"
    const val ID_SHUFFLE = "_shuffle"
    const val ID_BTN_REPEAT_MODE = "_btn_repeat_mode"
    const val ID_BTN_SKIP_PREVIOUS = "_btn_skip_previous"
    const val ID_BTN_PLAY_PAUSE = "_play_pause"
    const val ID_BTN_SKIP_TO_NEXT = "_skip_next"
    const val ID_SEEK_BAR = "_seek_bar"
    const val ID_VIDEO_SURFACE = "_video_surface"
    const val ID_TOAST = "_toast"
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
    // Actions
    const val ACTION_COLLAPSE = 0
    const val ACTION_TOGGLE_ROTATION_LOCK = 1
    const val ACTION_BACK_PRESS = 2

    // Ui Actions
    const val ACTION_SHOW_QUEUE = 3
    const val ACTION_SHOW_SLEEP_CONTROLLER = 4
    const val ACTION_SHOW_MEDIA_INFO = 5
    private const val ACTION_SHOW_NONE = 6

    // Background style
    const val STYLE_BG_SIMPLE = 0
    const val STYLE_BG_AMBIENT = 1
    // play button style
    const val STYLE_PLAY_BUTTON_SIMPLE = 0
    const val STYLE_PLAY_OUTLINED = 1
    const val STYLE_PLAY_BUTTON_TINTED = 2
    const val STYLE_PLAY_BUTTON_SOLID = 4

    @Composable
    operator fun invoke(viewState: ConsoleViewState) {
        val clazz = LocalWindowSize.current
        val facade = LocalSystemFacade.current
        val detailsOf by remember { mutableIntStateOf(ACTION_SHOW_NONE) }
        val insets = WindowInsets.systemBars
        CompositionLocalProvider(LocalContentColor provides AppTheme.colors.onBackground) {
            PlayerView(
                viewState = viewState,
                background = STYLE_BG_SIMPLE,
                insets = insets,
                onNewAction = { facade.showToast("Feature not implemented yet!!"); false },
                modifier = Modifier.sharedBounds(ID_BACKGROUND)
            )
        }
    }
}