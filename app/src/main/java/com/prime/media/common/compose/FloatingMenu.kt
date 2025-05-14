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

package com.prime.media.common.compose

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
import com.prime.media.common.Action
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.background
import com.zs.compose.foundation.thenIf
import com.zs.compose.theme.AppTheme
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
    insets: PaddingValues?= null,
    border: BorderStroke? = BorderStroke(
        0.5.dp,
        Brush.verticalGradient(
            listOf(
                if (AppTheme.colors.isLight) AppTheme.colors.background else Color.Gray.copy(0.24f),
                if (AppTheme.colors.isLight) AppTheme.colors.background.copy(0.3f) else Color.Gray.copy(0.075f),
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
    insets: PaddingValues?= null,
    border: BorderStroke? = BorderStroke(0.5.dp, Brush.verticalGradient(
            listOf(
                if (AppTheme.colors.isLight) AppTheme.colors.background else Color.Gray.copy(0.24f),
                if (AppTheme.colors.isLight) AppTheme.colors.background.copy(0.3f) else Color.Gray.copy(
                    0.075f
                ),
            )
        )),
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
 * A composable function that displays a row of menu items followed by a more button.
 *
 * @param items The list of menu items to display.
 * @param onItemClicked Callback invoked when a menu item is clicked.
 * @param collapsed The number of items in collapsed state in this state only icon is displayed.
 */
@Composable
inline fun RowScope.OverflowMenu(
    items: List<Action>,
    noinline onItemClicked: (item: Action) -> Unit,
    collapsed: Int = 2
) {
    val size = items.size
    // Early return if this has no items
    if (size == 0)
        return
    // Display the first 'collapsed' number of items as IconButtons
    repeat(minOf(size, collapsed)) { index ->
        val item = items[index]
        IconButton(
            icon = requireNotNull(item.icon) {
                "Collapsed Icon must not be null"
            }, // Icon is required for collapsed items
            onClick = { onItemClicked(item) },
            contentDescription = stringResource(item.label),
            enabled = item.enabled
        )
    }
    // If all items are already displayed, return
    if (size <= collapsed)
        return

    // State to control the expanded state of the dropdown menu
    val (expanded, onDismissRequest) = remember { mutableStateOf(false) }
    // IconToggleButton to show/hide the dropdown menu
    IconToggleButton(expanded, onDismissRequest) {
        Icon(Icons.Outlined.MoreVert, contentDescription = "more") // Icon for the "more" button

        // DropdownMenu to display the remaining items
        DropDownMenu(expanded, onDismissRequest = { onDismissRequest(false) }, modifier = Modifier.widthIn(min = 180.dp)) {
            repeat(size - collapsed) { index ->
                val item = items[index + collapsed]
                DropDownMenuItem(
                    title = stringResource(item.label),
                    onClick = { onItemClicked(item); onDismissRequest(false) },
                    icon = item.icon, // Icon is optional for dropdown items
                )
            }
        }
    }
}