/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 25-09-2024.
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

package com.zs.core_ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.primex.core.ExperimentalToolkitApi
import com.primex.core.shadow.SpotLight
import com.primex.core.shadow.shadow
import com.primex.material2.ProvideTextStyle
import com.primex.material2.appbar.CollapsableTopBarLayout
import com.primex.material2.appbar.LargeTopAppBar
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.primex.material2.appbar.TopAppBarStyle
import kotlin.math.roundToInt
import com.primex.material2.appbar.TopAppBarDefaults.LayoutIdAction as LAYOUT_ID_ACTIONS
import com.primex.material2.appbar.TopAppBarDefaults.LayoutIdBackground as LAYOUT_ID_BACKGROUND
import com.primex.material2.appbar.TopAppBarDefaults.LayoutIdCollapsable_title as LAYOUT_ID_COLLAPSABLE_TITLE
import com.primex.material2.appbar.TopAppBarDefaults.LayoutIdNavIcon as LAYOUT_ID_NAVIGATION_ICON

private const val TAG = "CollapsableNeumorphicLAB"


@Composable
@OptIn(ExperimentalToolkitApi::class)
inline fun TopAppBarDefaults.neumorphicTopBarStyle(
    containerColor: Color = AppTheme.colors.background,
    scrolledContainerColor: Color = Color.Transparent,
    contentColor: Color = AppTheme.colors.onBackground,
    scrolledContentColor: Color = AppTheme.colors.onBackground,
    titleTextStyle: TextStyle = AppTheme.typography.titleSmall,
    scrolledTitleTextStyle: TextStyle = AppTheme.typography.headlineLarge,
    height: Dp = TopBarHeight,
    maxHeight: Dp = LargeTopBarHeight,
) = largeAppBarStyle(
    containerColor = containerColor,
    scrolledContainerColor = scrolledContainerColor,
    contentColor = contentColor,
    scrolledContentColor = scrolledContentColor,
    titleTextStyle = titleTextStyle,
    scrolledTitleTextStyle = scrolledTitleTextStyle,
    height = height,
    maxHeight = maxHeight
)

@Stable
private fun TopAppBarStyle.titleTextStyle(fraction: Float): TextStyle {
    return lerp(
        titleTextStyle,
        scrolledTitleTextStyle,
        FastOutLinearInEasing.transform(fraction)
    )
}

@Stable
private fun TopAppBarStyle.contentColor(fraction: Float): Color {
    return androidx.compose.ui.graphics.lerp(
        contentColor,
        scrolledContentColor,
        FastOutLinearInEasing.transform(fraction)
    )
}

@Stable
private fun TopAppBarStyle.containerColor(fraction: Float): Color {
    return androidx.compose.ui.graphics.lerp(
        containerColor,
        scrolledContainerColor,
        FastOutLinearInEasing.transform(fraction)
    )
}

@Stable
private val TopAppBarStyle.background
    get() = Brush.verticalGradient(
        listOf(containerColor, containerColor.copy(0.9f), Color.Transparent)
    )

/**
 * A TopBar That collapses to Neumorphic app bar.
 */
@ExperimentalToolkitApi
@Composable
fun CollapsableNeumorphicLargeAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable() () -> Unit = {},
    actions: @Composable() RowScope.() -> Unit = {},
    insets: WindowInsets = WindowInsets.None,
    style: TopAppBarStyle = TopAppBarDefaults.neumorphicTopBarStyle(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CollapsableTopBarLayout(
        height = style.height,
        maxHeight = style.maxHeight,
        insets = insets,
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        content = {
            // Ensure that the height is properly set. The maxHeight must be greater than the height.
            require(style.height < style.maxHeight) {
                "CollapsableNeumorphicLargeAppBar maxHeight (${style.maxHeight}) must be greater than height (${style.height})"
            }
            // Provide styles to the content of the top app bar.
            ProvideTextStyle(
                color = style.contentColor(1 - fraction),
                style = style.titleTextStyle(fraction),
                content = {
                    // Background of the top app bar.
                    // It's a gradient background composed of the container color.
                    Spacer(
                        Modifier
                            .background(style.background)
                            .fillMaxSize()
                            .layoutId(LAYOUT_ID_BACKGROUND)
                    )
                    // Calculate the margin for the neumorphic surface based on the current scroll state.
                    val margin = androidx.compose.ui.unit.lerp(32.dp, 0.dp, fraction)
                    // Neumorphic surface of the top app bar.
                    // It has a shadow and a rounded corner shape.
                    Spacer(
                        modifier = Modifier
                            .padding(horizontal = margin)
                            .shadow(
                                RoundedCornerShape(((1 - fraction) * 100f).roundToInt()),
                                AppTheme.colors.lightShadowColor,
                                AppTheme.colors.darkShadowColor,
                                spotLight = SpotLight.TOP_LEFT,
                                // Add proper impl with support for elevation as well.
                                elevation = (1 - fraction) * 4.dp
                            )
                            .background(style.containerColor)
                            .requiredHeight(androidx.compose.ui.unit.lerp(46.dp, style.maxHeight, fraction))
                            .fillMaxWidth()
                    )

                    // Defines the navIcon and actions first;
                    // make sure that title is always last; because if it is not; a new list of
                    // measurables will be created; which will make sure it is at the last.
                    val scale = com.primex.core.lerp(0.85f, 1.0f, fraction)
                    Box(
                        Modifier
                            .scale(scale)
                            .layoutId(LAYOUT_ID_NAVIGATION_ICON)
                            .padding(start = TopAppBarDefaults.TopAppBarHorizontalPadding /** (1 - fraction)*/ + margin),
                        content = { navigationIcon() }
                    )

                    // Actions
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                        modifier = Modifier
                            .scale(scale)
                            .layoutId(LAYOUT_ID_ACTIONS)
                            .padding(end = TopAppBarDefaults.TopAppBarHorizontalPadding /** (1 - fraction)*/ + margin),
                    )

                    // Title
                    Box(
                        Modifier
                           //.scale(scale)
                            .layoutId(LAYOUT_ID_COLLAPSABLE_TITLE)
                            .padding(horizontal = TopAppBarDefaults.TopAppBarHorizontalPadding /** (1 - fraction)*/),
                        content = { title() }
                    )
                }
            )
        }
    )
}