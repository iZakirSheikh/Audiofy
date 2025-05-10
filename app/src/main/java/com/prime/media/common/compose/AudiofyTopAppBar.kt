/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-05-2025.
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

package com.prime.media.common.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.background
import com.zs.compose.foundation.thenIf
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Colors
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.appbar.FloatingLargeTopAppBar
import com.zs.compose.theme.appbar.LargeTopAppBar
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.appbar.TopAppBarScrollBehavior
import com.zs.compose.theme.appbar.TopAppBarStyle

private val FloatingTopBarShape = RoundedCornerShape(20)

private val Colors.border
    get() = BorderStroke(
        0.5.dp,
        Brush.verticalGradient(
            listOf(
                if (isLight) background else Color.Gray.copy(0.24f),
                if (isLight) background.copy(0.3f) else Color.Gray.copy(0.075f),
            )
        )
    )

/**
 * Represents the general purpose [TopAppBar] for screens.
 * @param immersive weather to load a topBar tha is end to end or floating.
 */
@Composable
@NonRestartableComposable
fun AudiofyTopAppBar(
    immersive: Boolean,
    title: @Composable () -> Unit,
    backdrop: Background,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    behavior: TopAppBarScrollBehavior,
    style: TopAppBarStyle = if (immersive) AppBarDefaults.largeAppBarStyle() else AppBarDefaults.floatingLargeAppBarStyle(),
    insets: WindowInsets = AppBarDefaults.topAppBarWindowInsets,
) = when {
    !immersive -> FloatingLargeTopAppBar(
        title = title,
        scrollBehavior = behavior,
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
                    .shadow(lerp(100.dp, 0.dp, fraction / .05f), FloatingTopBarShape)
                    .thenIf(fraction == 0f) {
                        border(colors.border, FloatingTopBarShape)
                    }
                    .background(backdrop)
                    .fillMaxSize()
            )
        }
    )

    else -> LargeTopAppBar(
        scrollBehavior = behavior,
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = insets,
        style = style,
        background = {
            if (fraction > 0.1f) return@LargeTopAppBar Spacer(Modifier)
            Spacer(
                modifier = Modifier
                    .background(backdrop)
                    .fillMaxSize()
            )
        }
    )
}