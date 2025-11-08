/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 17-10-2024.
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

package com.prime.media.common

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import com.primex.core.ImageBrush
import com.primex.core.foreground
import com.primex.core.visualEffect
import com.primex.material2.ProvideTextStyle
import com.primex.material2.appbar.CollapsableTopBarLayout
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.primex.material2.appbar.TopAppBarStyle


@Composable
fun ScenicAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable() () -> Unit = {},
    actions: @Composable() RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    background: @Composable BoxScope.() -> Unit,
    style: TopAppBarStyle = TopAppBarDefaults.largeAppBarStyle(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) = CollapsableTopBarLayout(
    height = style.height,
    maxHeight = style.maxHeight,
    insets = windowInsets,
    modifier = modifier.clipToBounds(),
    scrollBehavior = scrollBehavior
) {
    require(style.height < style.maxHeight) {
        "LargeTopAppBar maxHeight (${style.maxHeight}) must be greater than height (${style.height})"
    }
    ProvideTextStyle(style = style.titleTextStyle(fraction), color = style.contentColor(1 - fraction)) {
        // Background; zIndex determines which is stacked where.
        // so this will be at the bottom.
        val containerColor = style.containerColor(1 - fraction)
        // Background
        val curtain = lerp(containerColor, Color.Transparent, fraction)
        Box(
            modifier = Modifier
                .foreground(curtain)
                .visualEffect(ImageBrush.NoiseBrush, alpha = 0.35f, true)
                .parallax(0.2f)
                .layoutId(TopAppBarDefaults.LayoutIdBackground)
                .fillMaxSize(),
            content = background
        )
        //
        // FixMe - The gradient is also enlarged by parallax and hence this.
        val gradient = Brush.verticalGradient(colors = listOf(Color.Transparent, containerColor))
        Spacer(
            modifier = Modifier
                .alpha(com.primex.core.lerp(0f, 1f, fraction))
                .foreground(gradient)
                .fillMaxSize()
        )
        // Defines the navIcon and actions first;
        // make sure that title is always last; because if it is not; a new list of
        // measurables will be created; which will make sure it is at the last.
        Box(
            Modifier
                .layoutId(TopAppBarDefaults.LayoutIdNavIcon)
                .padding(start = TopAppBarDefaults.TopAppBarHorizontalPadding),
            content = { navigationIcon() }
        )

        // Actions
        Box(
            Modifier
                .layoutId(TopAppBarDefaults.LayoutIdAction)
                .padding(end = TopAppBarDefaults.TopAppBarHorizontalPadding),
            content = {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        )

        // title
        Box(
            Modifier
                .layoutId(TopAppBarDefaults.LayoutIdCollapsable_title)
                .padding(horizontal = TopAppBarDefaults.TopAppBarHorizontalPadding),
            content = { title() }
        )
    }
}