/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 14-05-2025.
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zs.audiofy.common.Action
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.background
import com.zs.compose.foundation.thenIf
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.HorizontalDivider
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.IconToggleButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.menu.DropDownMenu
import com.zs.compose.theme.menu.DropDownMenuItem

private val DefaultItemSpace = Arrangement.spacedBy(ContentPadding.small)

@Composable
@NonRestartableComposable
fun FloatingActionMenu(
    visible: Boolean,
    background: Background,
    modifier: Modifier = Modifier,
    contentColor: Color = AppTheme.colors.onBackground,
    insets: PaddingValues? = null,
    border: BorderStroke? = BorderStroke(
        0.5.dp,
        Brush.verticalGradient(
            listOf(
                if (AppTheme.colors.isLight) AppTheme.colors.background else Color.Gray.copy(0.24f),
                if (AppTheme.colors.isLight) AppTheme.colors.background.copy(0.3f) else Color.Gray.copy(
                    0.075f
                ),
            )
        )
    ),
    content: @Composable RowScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        content = {
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                content = {
                    Row(
                        horizontalArrangement = DefaultItemSpace,
                        verticalAlignment = Alignment.CenterVertically,
                        content = content,
                        modifier = Modifier
                            .pointerInput(Unit) {}
                            .thenIf(insets != null) { padding(insets!!) }
                            .scale(0.85f)
                            .shadow(12.dp, shape = CircleShape)
                            .thenIf(border != null) { border(border!!, CircleShape) }
                            .background(background)
                            .then(modifier)
                            .animateContentSize()
                    )
                }
            )
        }
    )
}

@Composable
fun FloatingActionMenu(
    background: Background,
    modifier: Modifier = Modifier,
    contentColor: Color = AppTheme.colors.onBackground,
    insets: PaddingValues? = null,
    border: BorderStroke? = BorderStroke(
        0.5.dp, Brush.verticalGradient(
            listOf(
                if (AppTheme.colors.isLight) AppTheme.colors.background else Color.Gray.copy(0.24f),
                if (AppTheme.colors.isLight) AppTheme.colors.background.copy(0.3f) else Color.Gray.copy(
                    0.075f
                ),
            )
        )
    ),
    content: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        content = {
            Row(
                horizontalArrangement = DefaultItemSpace,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
                modifier = Modifier
                    .pointerInput(Unit) {}
                    .thenIf(insets != null) { padding(insets!!) }
                    .scale(0.85f)
                    .shadow(12.dp, shape = CircleShape)
                    .thenIf(border != null) { border(border!!, CircleShape) }
                    .background(background)
                    .then(modifier)
                    .animateContentSize()
            )
        }
    )
}


/**
 * A composable function that displays a row of quick-access action items,
 * followed by an overflow menu button (three dots) that reveals remaining actions.
 *
 * The items are divided based on their visual presentation:
 * - **Collapsed items**: Displayed directly as icons in the horizontal row.
 * - **Expanded items**: Displayed in a dropdown menu shown when clicking the "more" button.
 *
 * The behavior is controlled by the `collapsed` and `expanded` parameters:
 *
 * @param items The complete list of actions to show (each action may include a label, icon, etc.).
 * @param onItemClicked A callback invoked when any menu item (either row or dropdown) is selected.
 * @param collapsed Number of items to show directly in the row. Only their icons are shown.
 * @param expanded Controls how remaining items are divided inside the dropdown:
 *  - `-1`: All remaining items are shown as menu entries in the dropdown.
 *  - Positive: This many items are shown as top menu items; the rest are icons in a row at the bottom.
 *  - Negative: The absolute value is shown as icons in a row at the top; the rest go into the menu below.
 */
@Composable
inline fun RowScope.OverflowMenu(
    items: List<Action>,
    noinline onItemClicked: (item: Action) -> Unit,
    collapsed: Int = 2,
    expanded: Int = -1
) {
    val size = items.size
    if (size == 0) return // Nothing to render

    // Show first `collapsed` items as IconButtons directly in the row
    repeat(minOf(size, collapsed)) { index ->
        val item = items[index]
        IconButton(
            icon = item.icon ?: Icons.Outlined.BrokenImage, // Fallback for missing icons
            onClick = { onItemClicked(item) },
            contentDescription = stringResource(item.label),
            enabled = item.enabled
        )
    }

    // All items already shown, no need for dropdown
    if (size <= collapsed) return

    // Dropdown menu toggle state
    val (show, onDismissRequest) = remember { mutableStateOf(false) }

    // "More" button to toggle dropdown visibility
    IconToggleButton(checked = show, onCheckedChange = { onDismissRequest(it) }) {
        Icon(Icons.Outlined.MoreVert, contentDescription = "More actions")

        // DropdownMenu: shows remaining items beyond the collapsed count
        DropDownMenu(
            expanded = show,
            onDismissRequest = { onDismissRequest(false) },
            modifier = Modifier.widthIn(min = 180.dp)
        ) {
            val remaining = size - collapsed

            when {
                // Case 1: Show all remaining items as dropdown entries
                expanded == -1 -> {
                    repeat(remaining) { index ->
                        val item = items[collapsed + index]
                        DropDownMenuItem(
                            title = stringResource(item.label),
                            onClick = {
                                onItemClicked(item)
                                onDismissRequest(false)
                            },
                            icon = item.icon
                        )
                    }
                }

                // Case 2: Show `expanded` items in dropdown and rest as row icons at bottom
                expanded > 0 -> {
                    val dropdownCount = minOf(remaining, expanded)
                    val overflowCount = remaining - dropdownCount

                    // Top section: Dropdown menu items
                    repeat(dropdownCount) { index ->
                        val item = items[collapsed + index]
                        DropDownMenuItem(
                            title = stringResource(item.label),
                            onClick = {
                                onItemClicked(item)
                                onDismissRequest(false)
                            },
                            icon = item.icon
                        )
                    }

                    // Divider if needed between dropdown and bottom row
                    if (overflowCount > 0) {
                        HorizontalDivider()
                    } else return@DropDownMenu

                    // Bottom section: Row of icon buttons inside dropdown
                    Row {
                        repeat(overflowCount) { index ->
                            val item = items[collapsed + dropdownCount + index]
                            IconButton(
                                icon = requireNotNull(item.icon) { "Collapsed Icon must not be null" },
                                onClick = { onItemClicked(item); onDismissRequest(false) },
                                contentDescription = stringResource(item.label),
                                enabled = item.enabled
                            )
                        }
                    }
                }

                // Case 3: Negative `expanded`: Row icons first, then remaining as dropdown items
                else -> {
                    val dropdownCount = minOf(remaining, -expanded)
                    val overflowCount = remaining - dropdownCount

                    // Bottom section: Row of icon buttons inside dropdown
                    if (overflowCount > 0) {
                        Row {
                            repeat(overflowCount) { index ->
                                val item = items[collapsed + dropdownCount + index]
                                IconButton(
                                    icon = requireNotNull(item.icon) { "Collapsed Icon must not be null" },
                                    onClick = { onItemClicked(item); onDismissRequest(false) },
                                    contentDescription = stringResource(item.label),
                                    enabled = item.enabled
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    // Bottom section: Dropdown menu items
                    repeat(dropdownCount) { index ->
                        val item = items[collapsed + index]
                        DropDownMenuItem(
                            title = stringResource(item.label),
                            onClick = {
                                onItemClicked(item)
                                onDismissRequest(false)
                            },
                            icon = item.icon
                        )
                    }
                }
            }
        }
    }
}