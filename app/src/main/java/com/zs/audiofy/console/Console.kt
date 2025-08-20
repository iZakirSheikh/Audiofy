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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.sharedBounds

@Composable
fun Console(viewState: ConsoleViewState) {
    val clazz = LocalWindowSize.current
    val facade = LocalSystemFacade.current
    val detailsOf by remember { mutableIntStateOf(/*ACTION_SHOW_NONE*/ 0) }
    val insets = WindowInsets.systemBars
    PlayerView(
        viewState = viewState,
        insets = insets,
        onNewAction = { facade.showToast("Feature not implemented yet!!"); false },
        modifier = Modifier.sharedBounds(RouteConsole.ID_BACKGROUND)
    )
}
