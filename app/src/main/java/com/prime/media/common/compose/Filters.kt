/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 17-04-2025.
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prime.media.common.Action
import com.prime.media.common.Filter
import com.zs.compose.foundation.composableIf
import com.zs.compose.foundation.fadingEdge
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Chip
import com.zs.compose.theme.ChipDefaults
import com.zs.compose.theme.Icon
import com.zs.compose.theme.SelectableChip
import com.zs.compose.theme.text.Label

private val REC_SPACING =
    Arrangement.spacedBy(ContentPadding.small)

// TODO - Migrate to LazyRow instead.

/**
 * Represents a [Row] of [Chip]s for ordering and filtering.
 *
 * @param current The currently selected filter.
 * @param values The list of supported filter options.
 * @param onRequest Callback function to be invoked when a filter option is selected. null
 * represents ascending/descending toggle.
 */
@Composable
fun Filters(
    current: Filter,
    values: List<Action>,
    onRequest: (order: Action?) -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    // Early return if values are empty.
    if (values.isEmpty()) return
    // TODO - Migrate to LazyRow
    val state = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
            .fadingEdge(/*AppTheme.colors.background, */state, true)
            .horizontalScroll(state),
        horizontalArrangement = REC_SPACING,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            // Chip for ascending/descending order
            val (ascending, order) = current
            val padding = PaddingValues(vertical = 6.dp)
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
                    backgroundColor = AppTheme.colors.accent,
                    contentColor = AppTheme.colors.onAccent
                ),
                modifier = Modifier
                    .padding(end = ContentPadding.medium),
                shape = AppTheme.shapes.small
            )

            // Rest of the chips for selecting filter options
            val colors = ChipDefaults.selectableChipColors(
                backgroundColor = AppTheme.colors.background(0.5.dp),
                selectedBackgroundColor = AppTheme.colors.background(2.dp),
                selectedContentColor = AppTheme.colors.accent,
                selectedLeadingIconColor = AppTheme.colors.accent
            )

            for (value in values) {
                val selected = value == order
                val label = stringResource(value.label)
                SelectableChip(
                    selected = selected,
                    onClick = { onRequest(value) },
                    content = {
                        Label(label, modifier = Modifier.padding(padding))
                    },
                    leadingIcon = composableIf (value.icon != null){
                        Icon(value.icon!!, contentDescription = label.toString())
                    },
                    colors = colors,
                    border = if (!selected) null else BorderStroke(
                        0.5.dp,
                        AppTheme.colors.accent.copy(0.12f)
                    ),
                    shape = CircleShape
                )
            }
        }
    )
}