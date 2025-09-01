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

package com.zs.audiofy.console


import android.view.Gravity
import androidx.annotation.FloatRange
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.shine
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.Dialog
import com.zs.compose.foundation.fadingEdge
import com.zs.compose.foundation.textArrayResource
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Chip
import com.zs.compose.theme.ChipDefaults
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.Slider
import com.zs.compose.theme.Surface
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Label
import kotlin.math.roundToInt
import com.zs.audiofy.common.compose.ContentPadding as CP

private val TitleBarHeight = Modifier.height(48.dp)
private val DialogSize = Modifier
    .widthIn(max = 400.dp)
    .padding(horizontal = CP.normal)

@Composable
private inline fun Layout(
    noinline navigationIcon: @Composable () -> Unit,
    noinline title: @Composable () -> Unit,
    noinline actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    noinline content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    val isLight = colors.isLight
    Surface(
        color = if (isLight) colors.accent else colors.background(3.dp),
        shape = AppTheme.shapes.xLarge,
        modifier = modifier then DialogSize,
        border = if (isLight) null else colors.shine
    ) {
        Column {
            // Top AppBar
            TopAppBar(
                navigationIcon = navigationIcon,
                title = title,
                background = Background(Color.Transparent),
                elevation = 0.dp,
                contentColor = LocalContentColor.current,
                modifier = TitleBarHeight,
                actions = actions
            )

            // content
            Surface(
                color = colors.background(1.dp),
                shape = AppTheme.shapes.xLarge,
                modifier = Modifier.padding(horizontal = 2.dp),
                content = {
                    Column(
                        content = content,
                        verticalArrangement = CP.SmallArrangement,
                        modifier = Modifier.padding(horizontal = CP.medium, vertical = CP.normal)
                    )
                }
            )
        }
    }
}

private val CustomWidthProperties = DialogProperties(usePlatformDefaultWidth = false)
private val SPEED_RANGE = 0.25f..3.0f
private const val SPEED_INCREMENT = 0.25f
private const val REQUEST_DISMISS = -1.0f

/**
 * Represents the playback speed dialog.
 * @param onRequestChange Called when the playback speed is changed. -1; dismiss the dialog.
 */
@Composable
fun PlaybackSpeed(
    expanded: Boolean,
    @FloatRange(0.25, 3.0) value: Float,
    onRequestChange: (value: Float) -> Unit
) {
    val (width, height) = LocalWindowSize.current
    val onDismissRequest = { onRequestChange(REQUEST_DISMISS) }

    Dialog(expanded, onDismissRequest = onDismissRequest, properties = CustomWidthProperties) {
        val view = LocalView.current
        SideEffect {
            val dialogWindowProvider = view.parent as? DialogWindowProvider ?: return@SideEffect
            val gravity = if (width > height) Gravity.CENTER else Gravity.BOTTOM
            val window = dialogWindowProvider.window
            window.setGravity(gravity)
        }

        Layout(
            navigationIcon = {
                IconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = null,
                    onClick = onDismissRequest
                )
            },
            title = {
                Label(
                    textResource(R.string.scr_playback_speed_title),
                    fontWeight = FontWeight.Light
                )
            },
            actions = {},
            content = {
                val (speed, onValueChange) = remember { mutableFloatStateOf(value) }
                // Slider to change the playback speed.

                Label(
                    text = textResource(R.string.scale_factor_f, speed),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = AppTheme.typography.display3
                )

                val colors = AppTheme.colors
                val isLight = colors.isLight
                // Controls
                val chipColors = ChipDefaults.chipColors(
                    backgroundColor = if (isLight) colors.background(6.dp) else colors.onBackground.copy(
                        ContentAlpha.indication
                    ),
                    contentColor = AppTheme.colors.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Decrease
                    Chip(
                        content = { Icon(Icons.Outlined.Remove, contentDescription = null) },
                        colors = chipColors,
                        border = ChipDefaults.outlinedBorder,
                        onClick = {
                            val newValue =
                                (speed - SPEED_INCREMENT).coerceAtLeast(SPEED_RANGE.start)
                            onValueChange(newValue)
                            onRequestChange(newValue)
                        },
                    )

                    // Slider
                    Slider(
                        value = speed,
                        onValueChange = onValueChange,
                        onValueChangeFinished = { onRequestChange(speed) },
                        valueRange = SPEED_RANGE,
                        steps = 11,
                        modifier = Modifier.weight(1f)
                    )

                    // Increase
                    Chip(
                        content = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        colors = chipColors,
                        border = ChipDefaults.outlinedBorder,
                        onClick = {
                            val newValue =
                                (speed + SPEED_INCREMENT).coerceAtMost(SPEED_RANGE.endInclusive)
                            onValueChange(newValue)
                            onRequestChange(newValue)
                        },
                    )
                }

                // Presets
                Header(stringResource(R.string.presets), style = AppTheme.typography.label3)
                val presetsScrollState = rememberScrollState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = CP.SmallArrangement,
                    modifier = Modifier
                        .fadingEdge(presetsScrollState, true, 15.dp)
                        .horizontalScroll(presetsScrollState),
                    content = {
                        val array = textArrayResource(id = R.array.scr_playback_speed_presets)
                        val padding = Modifier.padding(horizontal = CP.small)
                        for (index in array.indices) {
                            Chip(
                                colors = chipColors,
                                border = ChipDefaults.outlinedBorder,
                                content = { Label(text = array[index], modifier = padding) },
                                onClick = {
                                    // map index to value.
                                    val value = when (index) {
                                        0 -> 0.8f
                                        1 -> 1.0f
                                        2 -> 1.2f
                                        3 -> 1.5f
                                        4 -> 2.0f
                                        else -> error("")
                                    }
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
}


private val TIMER_RANGE = 10f..120f
private const val TIMER_INCREMENT = 10f

/**
 * Represents the sleep timer dialog.
 */
@Composable
fun SleepTimer(
    expanded: Boolean,
    onRequestChange: (value: Long) -> Unit
) {
    val (width, height) = LocalWindowSize.current
    val onDismissRequest = { onRequestChange(REQUEST_DISMISS.toLong()) }
    Dialog(expanded, onDismissRequest = onDismissRequest, properties = CustomWidthProperties) {
        // Handles the logic for showing dialog at bottom/centre
        val view = LocalView.current
        SideEffect {
            val dialogWindowProvider = view.parent as? DialogWindowProvider ?: return@SideEffect
            val gravity = if (width > height) Gravity.CENTER else Gravity.BOTTOM
            val window = dialogWindowProvider.window
            window.setGravity(gravity)
        }

        Layout(
            actions = {},
            navigationIcon = {
                IconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = null,
                    onClick = onDismissRequest
                )
            },
            title = {
                Label(
                    textResource(R.string.scr_sleep_timer_title),
                    fontWeight = FontWeight.Light
                )
            },
            content = {
                val (mins, onTimerChange) =  remember { mutableFloatStateOf(TIMER_RANGE.start) }
                // Preview
                Label(
                    text = textResource(R.string.scr_sleep_timer_minute_d, mins.roundToInt()),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = AppTheme.typography.title1
                )

                val colors = AppTheme.colors
                val isLight = colors.isLight
                // Controls

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = CP.SmallArrangement) {
                    val chipColors = ChipDefaults.chipColors(
                        backgroundColor = if (isLight) colors.background(6.dp) else colors.onBackground.copy(ContentAlpha.indication),
                        contentColor = colors.onBackground
                    )
                    // Decrease
                    Chip(
                        content = { Icon(Icons.Outlined.Remove, contentDescription = null) },
                        colors = chipColors,
                        border = ChipDefaults.outlinedBorder,
                        onClick = {
                            val newValue = (mins - TIMER_INCREMENT).coerceAtLeast(TIMER_RANGE.start)
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
                        border = ChipDefaults.outlinedBorder,
                        onClick = {
                            val newValue =
                                (mins + TIMER_INCREMENT).coerceAtMost(TIMER_RANGE.endInclusive)
                            onTimerChange(newValue)
                        },
                    )
                }

                // Start Timer
                Chip(modifier = Modifier.align(Alignment.End), onClick = {onRequestChange(mins.toLong() * 60_000)}) {
                    Label(stringResource(R.string.start).uppercase())
                }
            }
        )
    }
}