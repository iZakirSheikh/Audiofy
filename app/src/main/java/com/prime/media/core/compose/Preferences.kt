package com.prime.media.core.compose

import android.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.primex.core.composableOrNull
import com.primex.core.rememberState
import com.primex.material2.Label
import com.primex.material2.Preference
import com.primex.material2.Text


@Composable
private fun TextButtons(
    modifier: Modifier = Modifier,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit
) {

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancelClick) {
            Label(text = androidx.compose.ui.res.stringResource(id = R.string.cancel))
        }

        TextButton(onClick = onConfirmClick) {
            Label(text = androidx.compose.ui.res.stringResource(id = R.string.ok))
        }
    }
}

@Composable
fun SliderPreference2(
    title: CharSequence,
    defaultValue: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    singleLineTitle: Boolean = true,
    iconSpaceReserved: Boolean = true,
    icon: ImageVector? = null,
    summery: CharSequence? = null,
    forceVisible: Boolean = false,
    iconChange: ImageVector? = null,
    preview: CharSequence? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {

    val revealable =
        @Composable {
            val startPadding = (if (iconSpaceReserved) 24.dp + 16.dp else 0.dp) + 8.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = startPadding)
            ) {
                // place slider
                var value by rememberState(initial = defaultValue)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (iconChange != null)
                        Icon(
                            imageVector = iconChange,
                            contentDescription = null,
                        )
                    Slider(
                        value = value,
                        onValueChange = {
                            value = it
                        },
                        valueRange = valueRange,
                        steps = steps,
                        modifier = Modifier.weight(1f)
                    )
                    if (iconChange != null) {
                        Icon(
                            imageVector = iconChange,
                            contentDescription = null,
                            modifier = Modifier.scale(1.5f)
                        )
                    }
                }

                val manager = LocalFocusManager.current
                val onCancelClick = {
                    if (!forceVisible)
                        manager.clearFocus(true)
                }
                val onConfirmClick = {
                    if (!forceVisible)
                        manager.clearFocus(true)
                    onValueChange(value)
                }
                TextButtons(
                    onCancelClick = onCancelClick,
                    onConfirmClick = onConfirmClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

    Preference(
        modifier = modifier,
        title = title,
        enabled = enabled,
        singleLineTitle = singleLineTitle,
        iconSpaceReserved = iconSpaceReserved,
        icon = icon,
        forceVisible = forceVisible,
        summery = summery,
        widget = composableOrNull(preview != null) {
            Text(
                text = preview ?: "",
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(60.dp)
                    .wrapContentSize(Alignment.Center),
                textAlign = TextAlign.Center
            )
        },
        revealable = revealable
    )
}
