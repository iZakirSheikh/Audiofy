/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

@file:OptIn(ExperimentalMaterialApi::class)

package com.prime.media.old.console

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonDefaults.OutlinedBorderSize
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.zs.core_ui.ContentPadding as CP
import com.primex.core.drawHorizontalDivider
import com.primex.core.textResource
import com.primex.material2.Dialog
import com.primex.material2.Divider
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton
import com.primex.material2.TextButton
import com.zs.core_ui.AppTheme
import kotlin.math.roundToInt
import android.view.Gravity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Chip
import androidx.compose.material.ChipColors
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ChipDefaults.OutlinedBorderOpacity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.primex.core.fadingEdge
import com.primex.core.textArrayResource
import com.zs.core_ui.AlertDialog2
import com.zs.core_ui.Header
import com.zs.core_ui.Indication
import com.zs.core_ui.LocalWindowSize
import kotlin.math.roundToInt

private const val TAG = "ConsoleDialogs"

private val CustomWidthProperties = DialogProperties(usePlatformDefaultWidth = false)
private val SPEED_RANGE = 0.25f..8.0f
private const val REQUEST_DISMISS = -1.0f

private val PLAYBACK_SPEED_PRESETS =
    floatArrayOf(0.9f, 0.95f, 1.0f, 1.25f, 2.0f, 3.0f)

/**
 * Represents the playback speed dialog.
 * @param onRequestChange Called when the playback speed is changed. -1; dismiss the dialog.
 */
@Composable
fun PlaybackSpeed(
    expanded: Boolean,
    @FloatRange(0.25, 8.0) value: Float,
    onRequestChange: (value: Float) -> Unit
) {
    if (!expanded)
        return
    val onDismissRequest = { onRequestChange(REQUEST_DISMISS) }
    val (width, height) = LocalWindowSize.current
    //
    val step = 0.05f
    AlertDialog2(
        onDismissRequest = onDismissRequest,
        properties = CustomWidthProperties,
        gravity = if (width > height) Gravity.CENTER else Gravity.BOTTOM,
        title = {
            Label(
                textResource(R.string.playback_speed_dialog_title),
                style = AppTheme.typography.titleMedium
            )
        },
        navigationIcon = {
            IconButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                onClick = onDismissRequest
            )
        },
        content = {
            val (speed, onValueChange) = remember { mutableFloatStateOf(value) }
            // Slider to change the playback speed.
            // Preview
            Label(
                text = textResource(R.string.playback_speed_dialog_x_f, speed),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = AppTheme.typography.displaySmall
            )
            val colors = AppTheme.colors
            val isLight = colors.isLight
            // Controls
            val chipColors = ChipDefaults.chipColors(
                backgroundColor = if (isLight) colors.background(8.dp) else colors.onBackground.copy(
                    ContentAlpha.Indication
                ),
                contentColor = AppTheme.colors.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Decrease
                Chip(
                    content = { Icon(Icons.Outlined.Remove, contentDescription = null) },
                    colors = chipColors,
                    border = ChipDefaults.outlinedBorder,
                    shape = AppTheme.shapes.compact,
                    onClick = {
                        val newValue =
                            (speed - step).coerceAtLeast(SPEED_RANGE.start)
                        onValueChange(newValue)
                        onRequestChange(newValue)
                    },
                )

                // Slider
                Slider(
                    value = speed,
                    onValueChange = {
                        val newValue = (it / step).roundToInt() * step
                        onValueChange(newValue)
                    },
                    onValueChangeFinished = { onRequestChange(speed) },
                    valueRange = SPEED_RANGE,
                    modifier = Modifier.weight(1f)
                )

                // Increase
                Chip(
                    content = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    colors = chipColors,
                    shape = AppTheme.shapes.compact,
                    border = ChipDefaults.outlinedBorder,
                    onClick = {
                        val newValue =
                            (speed + step).coerceAtMost(SPEED_RANGE.endInclusive)
                        onValueChange(newValue)
                        onRequestChange(newValue)
                    },
                )
            }
            // Presets
            Header(stringResource(R.string.presets), style = AppTheme.typography.caption2)
            val presetsScrollState = rememberScrollState()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CP.small),
                modifier = Modifier
                    .fadingEdge(presetsScrollState, true, 15.dp)
                    .horizontalScroll(presetsScrollState),
                content = {
                    val padding = Modifier.padding(horizontal = CP.small)
                    for (value in PLAYBACK_SPEED_PRESETS) {
                        Chip(
                            colors = chipColors,
                            border = ChipDefaults.outlinedBorder,
                            shape = CircleShape,
                            content = {
                                Label(
                                    text = textResource(
                                        R.string.playback_speed_dialog_x_f,
                                        value
                                    ), modifier = padding
                                )
                            },
                            onClick = {
                                onRequestChange(value);
                                onValueChange(value)
                            },
                        )
                    }
                }
            )
        }
    )
}


private val TIMER_RANGE = 10f..120f
/**
 * Represents the sleep timer dialog.
 */
@Composable
fun SleepTimer(
    expanded: Boolean,
    onRequestChange: (mills: Long) -> Unit
) {
    if (!expanded) return
    val (width, height) = LocalWindowSize.current
    val onDismissRequest = { onRequestChange(REQUEST_DISMISS.toLong()) }
    val (mins, onTimerChange) = remember { mutableFloatStateOf(TIMER_RANGE.start) }
    val step = 10f
    AlertDialog2(
        onDismissRequest = onDismissRequest,
        properties = CustomWidthProperties,
        gravity = if (width > height) Gravity.CENTER else Gravity.BOTTOM,
        navigationIcon = {
            IconButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                onClick = onDismissRequest
            )
        },
        title = {
            Label(textResource(R.string.sleep_timer_dialog_title),  style = AppTheme.typography.titleMedium)
        },
        actions = {
            // Start Timer
            val color = LocalContentColor.current
            Chip(
                modifier = Modifier.padding(end = CP.small),
                colors = ChipDefaults.chipColors(
                    backgroundColor = color.copy(ContentAlpha.Indication),
                    contentColor = color
                ),
                shape = AppTheme.shapes.compact,
                onClick = { onRequestChange(mins.toLong() * 60_000) }) {
                Label(stringResource(R.string.start).uppercase())
            }
        },
        content = {
            // Preview
            Label(
                text = textResource(R.string.sleep_timer_dialog_minute_s, mins.roundToInt()),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = AppTheme.typography.titleLarge
            )

            val colors = AppTheme.colors
            val isLight = colors.isLight

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CP.small)
            ) {
                val chipColors = ChipDefaults.chipColors(
                    backgroundColor = if (isLight) colors.background(6.dp) else colors.onBackground.copy(
                        ContentAlpha.Indication
                    ),
                    contentColor = colors.onBackground
                )
                // Decrease
                Chip(
                    content = { Icon(Icons.Outlined.Remove, contentDescription = null) },
                    colors = chipColors,
                    shape = AppTheme.shapes.compact,
                    border = ChipDefaults.outlinedBorder,
                    onClick = {
                        val newValue = (mins - step).coerceAtLeast(TIMER_RANGE.start)
                        onTimerChange(newValue)
                    },
                )
                // Slider
                Slider(
                    value = mins,
                    onValueChange = onTimerChange,
                    valueRange = TIMER_RANGE,
                    steps = 10,
                    modifier = Modifier.weight(1f)
                )

                // Increase
                Chip(
                    content = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    colors = chipColors,
                    shape = AppTheme.shapes.compact,
                    border = ChipDefaults.outlinedBorder,
                    onClick = {
                        val newValue =
                            (mins + step).coerceAtMost(TIMER_RANGE.endInclusive)
                        onTimerChange(newValue)
                    },
                )
            }
        }
    )
}