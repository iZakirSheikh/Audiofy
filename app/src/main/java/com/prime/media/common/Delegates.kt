/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 11-10-2024.
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

package com.prime.media.common

import androidx.annotation.RawRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.SelectableChipColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prime.media.common.menu.Action
import com.primex.core.composableOrNull
import com.primex.material2.Label
import com.primex.material2.Text
import com.zs.core_ui.AppTheme
import com.zs.core_ui.adaptive.BottomNavItem
import com.zs.core_ui.adaptive.NavRailItem
import com.zs.core_ui.adaptive.NavigationItemDefaults
import com.zs.core_ui.lottieAnimationPainter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.rememberScrollState as ScrollState
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP

//This file holds the simple extension, utility methods of compose.
/**
 * Composes placeholder with lottie icon.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun Placeholder(
    title: String,
    modifier: Modifier = Modifier,
    vertical: Boolean = true,
    @RawRes iconResId: Int,
    message: CharSequence? = null,
    noinline action: @Composable (() -> Unit)? = null
) {
    com.primex.material2.Placeholder(
        modifier = modifier, vertical = vertical,
        message = composableOrNull(message != null) {
            Text(
                text = message!!,
                color = AppTheme.colors.onBackground,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        },
        title = {
            Label(
                text = title.ifEmpty { " " },
                maxLines = 2,
                color = AppTheme.colors.onBackground
            )
        },
        icon = {
            Image(
                painter = lottieAnimationPainter(id = iconResId),
                contentDescription = null
            )
        },
        action = action,
    )
}

/**
 * Item header.
 * //TODO: Handle padding in parent composable.
 */
private val HEADER_MARGIN = Padding(0.dp, CP.xLarge, 0.dp, CP.medium)
private val CHAR_HEADER_SHAPE = RoundedCornerShape(50, 25, 25, 25)

/**
 * Represents header for list/grid item groups.
 * Displays a single-character header as a circular "dew drop" or a multi-character header in a
 * two-line circular shape with a Material 3 background and subtle border.
 *
 * @param value The header text.
 * @param modifier The [Modifier] to apply.
 */
@NonRestartableComposable
@Composable
fun ListHeader(
    value: CharSequence,
    modifier: Modifier = Modifier
) {
    when {
        // If the value has only one character, display it as a circular header.
        // Limit the width of the circular header
        value.length == 1 -> Label(
            text = value,
            style = AppTheme.typography.headlineLarge,
            modifier = modifier
                .padding(HEADER_MARGIN)
                .border(0.5.dp, AppTheme.colors.background(20.dp), CHAR_HEADER_SHAPE)
                .background(AppTheme.colors.background(1.dp), CHAR_HEADER_SHAPE)
                .padding(horizontal = CP.large, vertical = CP.medium),
        )
        // If the value has more than one character, display it as a label.
        // Limit the label to a maximum of two lines
        // Limit the width of the label
        else -> Label(
            text = value,
            maxLines = 2,
            fontWeight = FontWeight.Normal,
            style = AppTheme.typography.titleSmall,
            modifier = modifier
                .padding(HEADER_MARGIN)
                .widthIn(max = 220.dp)
                .border(0.5.dp, AppTheme.colors.background(20.dp), CircleShape)
                .background(AppTheme.colors.background(1.dp), CircleShape)
                .padding(horizontal = CP.normal, vertical = CP.small)
        )
    }
}


private val ITEM_SPACING = Arrangement.spacedBy(CP.small)

/**
 * Represents a [Row] of [Chip]s for ordering and filtering.
 *
 * @param current The currently selected filter.
 * @param values The list of supported filter options.
 * @param onRequest Callback function to be invoked when a filter option is selected. null
 * represents ascending/descending toggle.
 */
// TODO - Migrate to LazyRow instead.
@Composable
fun Filters(
    current: Filter,
    values: List<Action>,
    padding: Padding = AppTheme.padding.None,
    onRequest: (order: Action?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Early return if values are empty.
    if (values.isEmpty()) return
    // TODO - Migrate to LazyRow
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
            .horizontalScroll(ScrollState()),
        horizontalArrangement = ITEM_SPACING,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            // Chip for ascending/descending order
            val (ascending, order) = current
            val padding = Padding(vertical = 6.dp)
            Chip(
                onClick = { onRequest(null) },
                content = {
                    Icon(
                        Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = "ascending",
                        modifier = Modifier.rotate(if (ascending) 0f else 180f)
                    )
                },
                colors = ChipDefaults.chipColors(
                    backgroundColor = AppTheme.colors.background(5.dp),
                    contentColor = AppTheme.colors.accent
                ),
                modifier = Modifier
                    .padding(end = CP.medium),
                shape = AppTheme.shapes.compact
            )
            // Rest of the chips for selecting filter options
            val colors = ChipDefaults.filterChipColors(
                backgroundColor = AppTheme.colors.background(0.5.dp),
                selectedBackgroundColor = AppTheme.colors.background(2.dp),
                selectedContentColor = AppTheme.colors.accent
            )

            for (value in values) {
                val selected = value == order
                val label = stringResource(value.label)
                FilterChip(
                    selected = selected,
                    onClick = { onRequest(value) },
                    content = {
                        Label(label, modifier = Modifier.padding(padding))
                    },
                    leadingIcon = composableOrNull(value.icon != null){
                        Icon(value.icon!!, contentDescription = label.toString())
                    },
                    colors = colors,
                    border = if (!selected) null else BorderStroke(
                        0.5.dp,
                        AppTheme.colors.accent.copy(0.12f)
                    ),
                    shape = AppTheme.shapes.compact
                )
            }
        }
    )
}

/**
 * Represents Navigation Item for navigation component.
 * @see NavRailItem
 * @see BottomNavItem
 */
@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalMaterialApi::class)
@Composable
inline fun NavItem(
    noinline onClick: () -> Unit,
    noinline icon: @Composable () -> Unit,
    noinline label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    typeRail: Boolean = false,
    colors: SelectableChipColors = NavigationItemDefaults.navigationItemColors(),
) = when (typeRail) {
    true -> NavRailItem(onClick, icon, label, modifier, checked, colors = colors)
    else -> BottomNavItem(onClick, icon, label, modifier, checked, colors = colors)
}