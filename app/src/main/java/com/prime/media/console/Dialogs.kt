@file:OptIn(ExperimentalTextApi::class)

package com.prime.media.console


import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Speed
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.menu.Popup2
import com.prime.media.small2
import com.prime.media.surfaceColorAtElevation
import com.primex.core.MetroGreen
import com.primex.core.textResource
import com.primex.material2.Dialog
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.TextButton
import kotlin.math.roundToInt

private const val TAG = "ConsoleDialogs"

@Composable
@NonRestartableComposable
private fun TopBar(
    onRequestTimerOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material.TopAppBar(
        title = {
            Label(
                text = textResource(R.string.sleep_timer_dialog_title),
                style = Material.typography.body1
            )
        },
        backgroundColor = Material.colors.surfaceColorAtElevation(1.dp),
        contentColor = Material.colors.onSurface,
        modifier = modifier,
        elevation = 0.dp,
        actions = {
            IconButton(imageVector = Icons.Outlined.TimerOff, onClick = onRequestTimerOff)
        }
    )
}


context(ColumnScope)
@Composable
private inline fun Layout(
    crossinline onValueChange: (value: Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var value by remember { mutableFloatStateOf(10f) }

    // Label
    Label(
        text = stringResource(R.string.sleep_timer_dialog_minute_s, value.roundToInt()),
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(top = ContentPadding.medium),
        style = Material.typography.h6,
    )

    Slider(
        value = value,
        onValueChange = {
            value = it
            // make phone vibrate.
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        valueRange = 10f..100f,
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(ContentPadding.normal),
        steps = 7
    )

    // Buttons
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ContentPadding.normal)
    ) {
        // In case it is running; it will stop it.
        TextButton(label = textResource(id = R.string.dismiss), onClick = { onValueChange(-2) })

        // start the timer.
        TextButton(
            label = textResource(id = R.string.start),
            onClick = { onValueChange((value.roundToInt() * 60 * 1_000L)) })
    }
}

@Composable
@NonRestartableComposable
fun SleepTimer(
    expanded: Boolean,
    onValueChange: (value: Long) -> Unit
) {
    Dialog(
        expanded = expanded,
        onDismissRequest = { onValueChange(-2) }
    ) {
        Surface(
            shape = Material.shapes.small2,
            content = {
                Column {
                    // TopBar
                    TopBar(onRequestTimerOff = { onValueChange(-1) })

                    // Content
                    Layout(onValueChange = onValueChange)
                }
            }
        )
    }
}


private val POP_UP_MAX_WIDTH = 250.dp

/**
 * Popup Composable for PlaybackSpeed settings.
 *
 * @param expanded Whether the settings are expanded.
 * @param value The current playback speed value.
 * @param onValueChange Callback for handling changes in playback speed.
 *                     If **-1f** is returned, it indicates a dismiss request.
 */
@Composable
@NonRestartableComposable
fun PlaybackSpeed(
    expanded: Boolean,
    @FloatRange(0.25, 2.0) value: Float,
    onValueChange: (value: Float) -> Unit
) {
    Popup2(
        expanded = expanded,
        onDismissRequest = { onValueChange(-1f) },
        shape = Material.shapes.small2,
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .widthIn(max = POP_UP_MAX_WIDTH)
            ) {
                // Representational Icon.
                // Slider to change the playback speed.
                val (state, callback) = remember { mutableFloatStateOf(value) }
                // Label
                Label(
                    text = stringResource(R.string.playback_speed_dialog_x_f, state),
                    fontWeight = FontWeight.Bold,
                    style = Material.typography.caption,
                    modifier = Modifier.padding(
                        start = ContentPadding.normal,
                        end = ContentPadding.medium
                    )
                )

                Slider(
                    value = state,
                    onValueChange = callback,
                    valueRange = 0.25f..2f,
                    modifier = Modifier.weight(1f),
                    steps = 6
                )

                IconButton(
                    imageVector = if (value != state) Icons.Outlined.DoneAll else Icons.Outlined.Speed,
                    // restore normal speed on click
                    onClick = { onValueChange(state) },
                    tint = if (value == state) Color.Unspecified else Color.MetroGreen,
                    enabled = value != state
                )
            }
        },
    )
}

