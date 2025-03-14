/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 20-07-2024.
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
@file:OptIn(ExperimentalMaterialApi::class)

package com.zs.core_ui.adaptive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.SelectableChipColors
import androidx.compose.material.Surface
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.primex.core.thenIf
import com.zs.core_ui.AppTheme

private val DefaultNavItemShape = RoundedCornerShape(20)

object NavigationItemDefaults {
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun navigationItemColors(
        backgroundColor: Color = Color.Transparent,
        contentColor: Color = LocalContentColor.current,
        selectedBackgroundColor: Color = AppTheme.colors.accent.copy(0.08f),
        selectedContentColor: Color = AppTheme.colors.accent,
        disabledBackgroundColor: Color = Color.Transparent,
        disabledContentColor: Color = contentColor.copy(ContentAlpha.disabled),
    ) = ChipDefaults.outlinedFilterChipColors(
        backgroundColor = backgroundColor,
        leadingIconColor = contentColor,
        disabledContentColor = disabledContentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledLeadingIconColor = disabledContentColor,
        selectedBackgroundColor = selectedBackgroundColor,
        selectedContentColor = selectedContentColor,
        selectedLeadingIconColor = selectedContentColor
    )

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun bottomNavItem2Colors(
        selectedIconColor: Color = AppTheme.colors.onAccent,
        selectedTextColor: Color = AppTheme.colors.accent,
        selectedIndicatorColor: Color = AppTheme.colors.accent,
        unselectedIconColor: Color = AppTheme.colors.onBackground,
        unselectedTextColor: Color = unselectedIconColor,
        disabledIconColor: Color = unselectedIconColor.copy(ContentAlpha.medium),
        disabledTextColor: Color = unselectedTextColor.copy(ContentAlpha.medium)
    )= ChipDefaults.outlinedFilterChipColors(
       // indicator
        backgroundColor = Color.Transparent,
        disabledBackgroundColor = Color.Transparent,
        selectedBackgroundColor = selectedIndicatorColor,
        // icon
        selectedContentColor = selectedIconColor,
        contentColor = unselectedIconColor,
        disabledContentColor = disabledIconColor,
        // text
        leadingIconColor = unselectedTextColor,
        disabledLeadingIconColor = disabledTextColor,
        selectedLeadingIconColor = selectedTextColor
    )
}

/**
 * A padding value used for checked components.
 */
private val CheckedPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/**
 * The size of the spacing between the leading icon and a text inside a chip.
 */
private val DefaultIconSpacing = 4.dp

/**
 * The content padding used by a Item when it's unchecked.
 */
private val UnCheckedPadding = PaddingValues(8.dp)

@Composable
fun BottomNavItem(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = DefaultNavItemShape,
    colors: SelectableChipColors = NavigationItemDefaults.navigationItemColors()
) {
    val contentColor by colors.contentColor(enabled, checked)
    val backgroundColor by colors.backgroundColor(enabled = enabled, selected = checked)
    Surface(
        onClick = onClick,
        shape = shape,
        selected = checked,
        enabled = enabled,
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier.animateContentSize(alignment = Alignment.CenterStart)
    ) {
        Row(
            Modifier
                .wrapContentSize()
                .padding(if (checked) CheckedPadding else UnCheckedPadding)
                .animateContentSize(),
            horizontalArrangement = if (checked) Arrangement.spacedBy(DefaultIconSpacing) else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leadingIconColor = colors.leadingIconColor(enabled, checked)
            CompositionLocalProvider(
                LocalContentColor provides leadingIconColor.value,
                LocalContentAlpha provides leadingIconColor.value.alpha,
                content = icon
            )
            // if not checked return else show label.
            if (!checked) return@Row
            CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
                ProvideTextStyle(
                    value = AppTheme.typography.caption,
                    label
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NavRailItem(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = DefaultNavItemShape,
    colors: SelectableChipColors = NavigationItemDefaults.navigationItemColors()
) {
    // This item necessitates the presence of both an icon and a label.
    // When selected, both the icon and label are displayed; otherwise, only the icon is shown.
    // The border is visible only when the item is selected; otherwise, it remains hidden.
    // The background color of this item is set to Color.Transparent.
    // TODO(b/113855296): Animate transition between unselected and selected
    val contentColor by colors.contentColor(enabled, checked)
    val backgroundColor by colors.backgroundColor(enabled = enabled, selected = checked)
    Surface(
        selected = checked,
        onClick = onClick,
        shape = shape,
        modifier = modifier.animateContentSize(alignment = Alignment.Center),
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        Column(
            Modifier
                .padding(UnCheckedPadding)
                .thenIf(checked) { defaultMinSize(48.dp) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (!checked) Arrangement.spacedBy(DefaultIconSpacing) else Arrangement.Center,
        ) {
            val leadingIconColor = colors.leadingIconColor(enabled, checked)
            CompositionLocalProvider(
                LocalContentColor provides leadingIconColor.value,
                LocalContentAlpha provides leadingIconColor.value.alpha,
                content = icon
            )
            // Label is only shown when checked.
            // return from here if not checked.
            if (!checked) return@Column
            CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
                ProvideTextStyle(
                    value = AppTheme.typography.caption2,
                    label
                )
            }
        }
    }
}


private val BottomBarItemMinSize = 80.dp


private val BottomBarIndicatorIconPadding =
    PaddingValues(horizontal = 16.dp, vertical = 2.dp)

private val IndicatorLabelArrangement =
    Arrangement.spacedBy(4.dp, Alignment.CenterVertically)

/**
 * Material Design navigation bar item.
 *
 * Navigation bars offer a persistent and convenient way to switch between primary destinations in
 * an app.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param label optional text label for this item
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this
 *   item in different states. See [NavigationItemDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun BottomNavItem2(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    colors: SelectableChipColors = NavigationItemDefaults.bottomNavItem2Colors()
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember(::MutableInteractionSource)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = IndicatorLabelArrangement,
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null
            )
            .defaultMinSize(minHeight = BottomBarItemMinSize)
    ) {
        val iconColor by colors.contentColor(enabled, selected)
        val indicatorColor by colors.backgroundColor(enabled, selected)
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .indication(interactionSource, ripple(bounded = false, color = iconColor))
                .background(indicatorColor)
                .padding(BottomBarIndicatorIconPadding),
            content = {
                CompositionLocalProvider(LocalContentColor provides iconColor, icon)
            }
        )

        /*Label*/
        val labelColor by colors.leadingIconColor (selected, enabled)
        CompositionLocalProvider(LocalContentColor provides labelColor) {
            ProvideTextStyle(AppTheme.typography.caption2, label)
        }
    }
}