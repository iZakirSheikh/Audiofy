/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 28-01-2025.
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Default radius of an unbounded ripple in an IconButton
private val RippleRadius = 24.dp

/**
 * An [IconButton] with two states, for icons that can be toggled 'on' and 'off', such as a bookmark
 * icon, or a navigation icon that opens a drawer.
 *
 * @sample androidx.compose.material.samples.IconToggleButtonSample
 * @param checked whether this IconToggleButton is currently checked
 * @param onCheckedChange callback to be invoked when this icon is selected
 * @param modifier optional [Modifier] for this IconToggleButton
 * @param enabled enabled whether or not this [IconToggleButton] will handle input events and appear
 *   enabled for semantics purposes
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this IconButton. You can use this to change the IconButton's
 *   appearance or preview the IconButton in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content (icon) to be drawn inside the IconToggleButton. This is typically an
 *   [Icon].
 */
@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Checkbox,
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = RippleRadius)
                ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor =
            if (enabled) LocalContentColor.current else LocalContentColor.current.copy(
                ContentAlpha.disabled
            )
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * @see IconToggleButton
 */
@Composable
@NonRestartableComposable
fun IconToggleButton(
    checked: Boolean,
    icon: ImageVector,
    contentDescription: String?,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) = IconToggleButton(
    checked, onCheckedChange, modifier, enabled, interactionSource
) {
    Icon(icon, contentDescription, tint = tint)
}


/**
 * TonalIconButton is a clickable icon with a background color, used to represent actions.
 *
 * This component is similar to [IconButton], but it has a background color that provides
 * a visual cue for interaction.
 *
 * @param onClick the lambda to be invoked when this icon is pressed
 * @param modifier optional [Modifier] for this TonalIconButton
 * @param enabled whether or not this TonalIconButton will handle input events and appear enabled for semantics purposes
 * @param color the color to be used for the background and content of this TonalIconButton. If [Color.Unspecified] is provided, [LocalContentColor] will be used.
 * @param shape the shape of the TonalIconButton's background
 * @param border optional [BorderStroke] to be applied to the TonalIconButton's background
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and emitting [Interaction]s for this TonalIconButton.
 * @param content the content (icon) to be drawn inside the TonalIconButton. This is typically an [Icon].
 */
@Composable
fun TonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
    shape: Shape = CircleShape,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val color = color.takeOrElse { LocalContentColor.current }
    val tint = color.copy(ContentAlpha.Indication)
    Box(
        modifier = modifier
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(tint, shape)
            .clip(shape)
            // Using MinimumInteractiveSize here seems too much.
            .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = RippleRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = color.copy(if (enabled) ContentAlpha.high else ContentAlpha.disabled)
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            content = content
        )
    }
}

/**
 * @see TonalIconButton
 */
@NonRestartableComposable
@Composable
fun TonalIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    color: Color = Color.Unspecified,
    shape: Shape = CircleShape,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
) = TonalIconButton(onClick, modifier, enabled, color, shape, border, interactionSource) {
    Icon(icon, contentDescription)
}