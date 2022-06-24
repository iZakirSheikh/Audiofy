package com.prime.player.extended

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Chip(
    label: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {

    val toggleableModifier =
        if (onCheckedChange != null) {
            Modifier.toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null
            )
        } else {
            Modifier
        }

    val alpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled

    val color by animateColorAsState(
        targetValue = if (enabled) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(
            ContentAlpha.disabled
        )
    )


    Frame(
        color = color.copy(0.1f),
        contentColor = color,
        shape = RoundedCornerShape(50),
        border = if (checked) BorderStroke(
            width = 1.dp,
            color = color
        ) else null,
        modifier = modifier.then(toggleableModifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Padding.LARGE, vertical = Padding.MEDIUM)
        ) {
            CompositionLocalProvider(LocalContentAlpha provides alpha) {
                icon?.let {
                    Image(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.padding(end = Padding.MEDIUM),
                    )
                }
                Caption(text = label, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}