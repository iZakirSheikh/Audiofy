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
import com.zs.core_ui.ContentPadding
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

private const val TAG = "ConsoleDialogs"

@Composable
@NonRestartableComposable
private fun TopBar(
    onRequestTimerOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Label(
                text = textResource(R.string.sleep_timer_dialog_title),
                style = AppTheme.typography.bodyLarge
            )
        },
        backgroundColor = AppTheme.colors.background(1.dp),
        contentColor = AppTheme.colors.onBackground,
        modifier = modifier,
        elevation = 0.dp,
        actions = {
            IconButton(imageVector = Icons.Outlined.TimerOff, onClick = onRequestTimerOff)
        }
    )
}


context(scope: ColumnScope)
@Composable
private inline fun Layout(
    crossinline onValueChange: (value: Long) -> Unit
) {
    with(scope, {
        val haptic = LocalHapticFeedback.current
        var value by remember { mutableFloatStateOf(10f) }

        // Label
        Label(
            text = stringResource(R.string.sleep_timer_dialog_minute_s, value.roundToInt()),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = ContentPadding.medium),
            style = AppTheme.typography.titleLarge,
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
    })
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
            shape = AppTheme.shapes.compact,
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


private val DIALOG_ITEM_PADDING = PaddingValues(
    top = ContentPadding.medium,
    start = ContentPadding.normal,
    end = ContentPadding.normal
)

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
    Dialog(
        expanded = expanded,
        onDismissRequest = { onValueChange(-1f) },
        backgroundColor = AppTheme.colors.background(0.3.dp),
        shape = AppTheme.shapes.compact,
        content = {
            Column {
                // TopBar
                TopAppBar(
                    title = {
                        Label(
                            text = textResource(id = R.string.playback_speed_dialog_title),
                            style = AppTheme.typography.bodyLarge
                        )
                    },
                    backgroundColor = Color.Transparent,
                    elevation = 0.dp,
                    // Dialog close button.
                    navigationIcon = {
                        IconButton(
                            imageVector = Icons.Outlined.Close,
                            onClick = { onValueChange(-1f) }
                        )
                    },
                    modifier = Modifier.drawHorizontalDivider(
                        AppTheme.colors.onBackground.copy(ContentAlpha.Divider),
                        indent = PaddingValues(horizontal = ContentPadding.normal)
                    )
                )

                // Slider to change the playback speed.
                val (state, callback) = remember { mutableFloatStateOf(value) }
                Label(
                    text = textResource(R.string.playback_speed_dialog_x_f, state),
                    modifier = Modifier
                        .padding(DIALOG_ITEM_PADDING)
                        .align(Alignment.CenterHorizontally),
                    style = AppTheme.typography.displaySmall,
                    fontWeight = FontWeight.Light
                )

                // Slider from 0.2 to 3.0 with 28 steps.
                Slider(
                    value = state,
                    onValueChange = callback,
                    valueRange = 0.2f..3f,
                    modifier = Modifier.padding(DIALOG_ITEM_PADDING),
                    steps = 28
                )

                // Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ContentPadding.medium),
                    modifier = Modifier
                        .padding(DIALOG_ITEM_PADDING)
                        .horizontalScroll(rememberScrollState()),
                    content = {
                        //
                        val colors =
                            ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
                        val border = BorderStroke(
                            OutlinedBorderSize,
                            AppTheme.colors.accent.copy(alpha = ButtonDefaults.OutlinedBorderOpacity)
                        )
                        val shape = CircleShape
                        val array =
                            stringArrayResource(id = R.array.playback_speed_dialog_predefined_speeds)
                        array.forEach { value ->
                            OutlinedButton(
                                label = value,
                                // FixMe - This is not the ideal way to do this. since value can be any char not just 1,2 etv.
                                onClick = { callback(value.toFloat()) },
                                shape = shape,
                                colors = colors,
                                border = border,
                            )
                        }
                    }
                )

                // Apply Button
                TextButton(
                    label = stringResource(id = R.string.apply),  // restore normal speed on click
                    onClick = { onValueChange(state) },
                    modifier = Modifier
                        .padding(DIALOG_ITEM_PADDING)
                        .align(Alignment.End),
                    // border = ButtonDefaults.outlinedBorder,
                    //colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
                )
            }
        }
    )
}

