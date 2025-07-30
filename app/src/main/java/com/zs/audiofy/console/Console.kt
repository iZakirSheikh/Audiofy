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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.Surface
import com.zs.compose.theme.sharedBounds

@Composable
private fun Controls(modifier: Modifier = Modifier) {

}

@Composable
fun Console(viewState: ConsoleViewState) {
    val clazz = LocalWindowSize.current
    val x by viewState.state.collectAsState(null)
    val state = x ?: return
    Surface (modifier = Modifier.fillMaxSize().sharedBounds(RouteConsole.SHARED_ELEMENT_BACKGROUND), color = Color.Blue) {
//        AsyncImage(
//            model = state.artwork,
//            contentDescription = null,
//            modifier = Modifier
//                .fillMaxSize()
//                .blur(95.dp)
//                .foreground(AppTheme.colors.background.copy(if (AppTheme.colors.isLight) 0.85f else 0.92f))
//                .visualEffect(ImageBrush.NoiseBrush, overlay = true),
//            contentScale = ContentScale.Crop
//        )
    }
}