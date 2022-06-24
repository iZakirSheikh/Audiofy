package com.prime.player.extended

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


@Composable
fun ColorPicker(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    entries: List<Color>,
    checked: MutableState<Color>,
) {
    var selected by checked
    Row(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (color in entries) {
            Spacer(
                modifier = Modifier
                    .graphicsLayer {
                        if (selected == color) {
                            scaleY = 1.15f
                        }
                    }
                    .size(width = 15.dp, height = 45.dp)
                    .weight(1f)
                    .background(color)
                    .clickable(enabled) {
                        selected = color
                    }
            )
        }
    }
}


@Composable
fun ColorPickerDialog(
    state: MutableState<Boolean>,
    entries: List<Color>,
    defaultEntry: Color,
    title: String,
    icon: ImageVector? = null,
    onRequestValueChange: (Color) -> Unit
) {
    val selected = androidx.compose.runtime.remember {
        mutableStateOf(defaultEntry)
    }

    PrimeDialog(
        title = title,
        onDismissRequest = { state.value = false },
        vectorIcon = icon,
        topBarBackgroundColor = selected.value,
        topBarContentColor = suggestContentColorFor(selected.value),
        button1 = stringResource(id = R.string.cancel) to {
            state.value = false
        },
        button2 = stringResource(id = R.string.ok) to {
            onRequestValueChange.invoke(selected.value)
            state.value = false
        }
    ) {
        ColorPicker(
            entries = entries,
            checked = selected,
            modifier = Modifier.requiredHeight(150.dp)
        )
    }
}

