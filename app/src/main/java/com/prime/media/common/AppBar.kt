/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-02-2025.
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.primex.core.blend
import com.primex.core.thenIf
import com.primex.material2.appbar.CollapsableTopBarLayout
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScope
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.primex.material2.appbar.TopAppBarStyle
import com.zs.core_ui.AppTheme
import com.zs.core_ui.None
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle

private const val FLTAB_COLLAPSED_HORIZONTAL_PADDING = 30f

/** @see largeAppBarStyle  */
@Composable
fun AppBarDefaults.floatingLargeAppBarStyle(
    containerColor: Color = AppTheme.colors.background,
    scrolledContainerColor: Color =  AppTheme.colors.accent.blend(AppTheme.colors.background, 0.96f),
    contentColor: Color = AppTheme.colors.onBackground,
    scrolledContentColor: Color = AppTheme.colors.onBackground,
    titleTextStyle: TextStyle = AppTheme.typography.titleMedium,
    scrolledTitleTextStyle: TextStyle = AppTheme.typography.headlineLarge,
    height: Dp = TopAppBarDefaults.TopBarHeight,
    maxHeight: Dp = TopAppBarDefaults.LargeTopBarHeight,
)= TopAppBarDefaults.largeAppBarStyle(
    containerColor = containerColor,
    scrolledContainerColor = scrolledContainerColor,
    contentColor = contentColor,
    scrolledContentColor = scrolledContentColor,
    titleTextStyle = titleTextStyle,
    scrolledTitleTextStyle = scrolledTitleTextStyle,
    height = height,
    maxHeight = maxHeight
)

private val FloatingTopBarShape = RoundedCornerShape(20)
private val FLOATING_TOP_APP_BAR_MAX_WIDTH = 500.dp
val AppBarDefaults.floatingTopBarShape get() = FloatingTopBarShape


/**
 * Returns the container color interpolated based on the provided scroll fraction.
 *
 * @param fraction A value between 0.0 and 1.0, indicating the scroll state:
 *     - 0.0: Fully collapsed
 *     - 1.0: Fully expanded or overlapped
 *
 * @return The interpolated container color, smoothly transitioning between
 *     `containerColor` and `scrolledContainerColor` using a fast-out-linear-in easing.
 */
@Stable
fun TopAppBarStyle.containerColor(fraction: Float): Color {
    return androidx.compose.ui.graphics.lerp(
        containerColor,
        scrolledContainerColor,
        FastOutLinearInEasing.transform(fraction)
    )
}

/**
 * @see containerColor
 */
@Stable
fun TopAppBarStyle.contentColor(fraction: Float): Color {
    return androidx.compose.ui.graphics.lerp(
        contentColor,
        scrolledContentColor,
        FastOutLinearInEasing.transform(fraction)
    )
}

/**
 * @see containerColor
 */
@Stable
fun TopAppBarStyle.titleTextStyle(fraction: Float): TextStyle {
    return androidx.compose.ui.text.lerp(
        titleTextStyle,
        scrolledTitleTextStyle,
        FastOutLinearInEasing.transform(fraction)
    )
}

@Stable
private fun elevation(elevation: Dp, fraction: Float): Dp {
    return lerp(
        elevation,
        0.dp,
        FastOutLinearInEasing.transform(fraction)
    )
}

/** @see LargeTopAppBar */
@Composable
fun FloatingLargeTopAppBar(
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable() () -> Unit = {},
    actions: @Composable() RowScope.() -> Unit = {},
    windowInsets: WindowInsets = AppBarDefaults.topAppBarWindowInsets,
    style: TopAppBarStyle = AppBarDefaults.floatingLargeAppBarStyle(),
    background: @Composable TopAppBarScope.() -> Unit = {
        val colors = AppTheme.colors
        Spacer(
            modifier = Modifier
                .shadow(lerp(12.dp, 0.dp, fraction / .05f), AppBarDefaults.floatingTopBarShape)
                .thenIf(fraction == 0f) {
                    border(
                        0.1.dp,
                        colors.background(30.dp),
                        AppBarDefaults.floatingTopBarShape
                    )
                }
                .background(style.containerColor(1 - fraction))
                .fillMaxSize()
        )
    }
) {
    // TODO - Expose fraction through behaviour; instead of relying here on state.
    var hPadding by rememberSaveable { mutableFloatStateOf(0f) }
    CollapsableTopBarLayout(
        height = style.height,
        maxHeight = style.maxHeight,
        insets = WindowInsets.None,
        modifier = modifier
            .widthIn(max = FLOATING_TOP_APP_BAR_MAX_WIDTH)
            .windowInsetsPadding(windowInsets)
            .padding(horizontal = hPadding.dp),
        scrollBehavior = scrollBehavior
    ) {
        require(style.height < style.maxHeight) {
            "LargeTopAppBar maxHeight (${style.maxHeight}) must be greater than height (${style.height})"
        }
        // update hPadding
        hPadding = androidx.compose.ui.util.lerp(FLTAB_COLLAPSED_HORIZONTAL_PADDING, 0f, fraction)
        val appBarContentColor = style.contentColor(1 - fraction)
        val textStyle = style.titleTextStyle(fraction)
        CompositionLocalProvider(LocalContentColor provides appBarContentColor) {
            ProvideTextStyle(textStyle) {
                // Background; zIndex determines which is stacked where.
                // so this will be at the bottom.
                Box(
                    modifier = Modifier.layoutId(TopAppBarDefaults.LayoutIdBackground),
                    content = { background() }
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

                Box(
                    Modifier
                        .layoutId(TopAppBarDefaults.LayoutIdCollapsable_title)
                        .padding(horizontal = TopAppBarDefaults.TopAppBarHorizontalPadding),
                    content = { title() }
                )
            }
        }
    }
}

@Composable
@NonRestartableComposable
fun FloatingLargeTopAppBar(
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    backdrop: HazeState?,
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
                .shadow(androidx.compose.ui.unit.lerp(100.dp, 0.dp, fraction / .05f), AppBarDefaults.floatingTopBarShape)
                .thenIf(fraction == 0f) {
                    border(colors.shine, AppBarDefaults.floatingTopBarShape)
                }
                .dynamicBackdrop(
                    backdrop,
                    HazeStyle.Regular(colors.background(0.4.dp)),
                    colors.background,
                    colors.accent
                )
                .fillMaxSize()
        )
    }
)