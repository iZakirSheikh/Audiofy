/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 05-03-2025.
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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.primex.core.shapes.SquircleShape
import com.primex.material2.Label

private val IndicatorShape = SquircleShape(0.7f)
private val DefaultIndicatorSize = Modifier.defaultMinSize(56.dp, 56.dp)

private val IconLabelSpace = Arrangement.spacedBy(8.dp)
private val AnimSpec = tween<Float>(
    durationMillis = Anim.MediumDurationMills,
    easing = FastOutSlowInEasing
)


/**
 * Represents a toggle button that provides a visual indicator for its selected state.
 *
 * This button displays an icon and an optional label. The indicator's color and the icon's size
 * change smoothly to reflect the selected state.
 *
 * @param selected `true` if the button is currently selected, `false` otherwise.
 * @param onClick The callback to be invoked when the button is clicked.
 * @param icon The composable representing the icon to be displayed inside the button's indicator.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    indicatorColor: Color = AppTheme.colors.accent,
    onIndicatorColor: Color = AppTheme.colors.onAccent
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = IconLabelSpace,
        content = {
            val fraction by animateFloatAsState(if (selected) 1f else 0f, AnimSpec)

            val newIndicatorColor = if (enabled)
                indicatorColor else indicatorColor.copy(
                ContentAlpha.disabled
            )
            val newOnIndicatorColor = if (enabled) onIndicatorColor else onIndicatorColor.copy(
                ContentAlpha.disabled
            )

            Surface(
                selected = selected,
                modifier = DefaultIndicatorSize,
                color = lerp(Color.Transparent, newIndicatorColor, fraction),
                shape = IndicatorShape,
                contentColor = lerp(newIndicatorColor, newOnIndicatorColor, fraction),
                onClick = onClick,
                enabled = enabled,
                border = if (!selected) ButtonDefaults.outlinedBorder else null,
                interactionSource = interactionSource,
                content = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.scale(
                            lerp(1.0f, 1.3f, fraction)
                        ),
                        content = { icon() }
                    )
                }
            )

            if (label == null) return
            val labelColor =
                if (enabled) AppTheme.colors.onBackground else AppTheme.colors.onBackground.copy(
                    ContentAlpha.disabled
                )
            CompositionLocalProvider(
                LocalTextStyle provides AppTheme.typography.caption,
                LocalContentColor provides labelColor,
                content = label
            )
        }
    )
}


/**  @see ToggleButton */
@Composable
@NonRestartableComposable
fun ToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: CharSequence? = null,
    interactionSource: MutableInteractionSource? = null,
    indicatorColor: Color = AppTheme.colors.accent,
    onIndicatorColor: Color = AppTheme.colors.onAccent
) = ToggleButton(
    selected = selected,
    onClick = onClick,
    icon = { Icon(icon, contentDescription = null) },
    modifier = modifier,
    enabled = enabled,
    label = label?.let { { Label(it) } },
    interactionSource = interactionSource,
    indicatorColor = indicatorColor,
    onIndicatorColor = onIndicatorColor
)

