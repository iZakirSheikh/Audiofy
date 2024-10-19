/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 20-10-2024.
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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.zs.core_ui.AppTheme

private val DefaultItemSpace =  Arrangement.spacedBy(AppTheme.padding.small)

/**
 * A composable function that creates a floating action menu.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param content The composable content to be displayed within the menu.
 */
@Composable
@NonRestartableComposable
fun FloatingActionMenu(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.scale(0.85f),
        color = AppTheme.colors.background(elevation = 2.dp),
        contentColor = AppTheme.colors.onBackground,
        shape = CircleShape,
        border = BorderStroke(1.dp, AppTheme.colors.background(elevation = 4.dp)),
        elevation = 12.dp,
        content = {
            Row(
                horizontalArrangement = DefaultItemSpace,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.animateContentSize(),
                content = content
            )
        }
    )
}