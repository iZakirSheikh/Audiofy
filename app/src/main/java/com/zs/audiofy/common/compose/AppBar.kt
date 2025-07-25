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

package com.zs.audiofy.common.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.background
import com.zs.compose.foundation.thenIf
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.appbar.FloatingLargeTopAppBar
import com.zs.compose.theme.appbar.TopAppBarScrollBehavior
import com.zs.compose.theme.appbar.TopAppBarStyle

@Composable
@NonRestartableComposable
fun FloatingLargeTopAppBar(
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    background: Background,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable() () -> Unit = {},
    actions: @Composable() RowScope.() -> Unit = {},
    insets: WindowInsets = AppBarDefaults.topAppBarWindowInsets,
    style: TopAppBarStyle = AppBarDefaults.floatingLargeAppBarStyle(),
) = FloatingLargeTopAppBar(
    title = title,
    scrollBehavior = scrollBehavior,
    modifier = modifier,
    navigationIcon = navigationIcon,
    actions = actions,
    windowInsets = insets,
    style = style,
    background = {
        if (fraction > 0.1f) return@FloatingLargeTopAppBar Spacer(Modifier)
        val colors = AppTheme.colors
        Spacer(
            modifier = Modifier
                .shadow(androidx.compose.ui.unit.lerp(100.dp, 0.dp, fraction / .05f), AppBarDefaults.FloatingTopBarShape)
                .thenIf(fraction == 0f) {
                    border(colors.shine, AppBarDefaults.FloatingTopBarShape)
                }
                .background(background)
                .fillMaxSize()
        )
    }
)