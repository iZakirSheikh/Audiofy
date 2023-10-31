@file:OptIn(ExperimentalTextApi::class)

package com.prime.media.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TimerOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.ContentPadding
import com.prime.media.small2
import com.prime.media.surfaceColorAtElevation
import com.primex.core.OrientRed
import com.primex.core.textResource
import com.primex.material2.Dialog
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton
import com.primex.material2.TextButton
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val TAG = "Timer"

@Composable
@NonRestartableComposable
private fun TopBar(
    onRequestTimerOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material.TopAppBar(
        title = { Label(text = textResource(R.string.sleep_timer_dialog_title), style = Material.typography.body1) },
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
fun Timer(
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