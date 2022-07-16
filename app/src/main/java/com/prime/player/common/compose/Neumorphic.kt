package com.prime.player.common.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prime.player.Material
import com.prime.player.darkShadowColor
import com.prime.player.lightShadowColor
import com.primex.core.shadow.SpotLight
import com.primex.core.shadow.shadow

private val DefaultNeumorphicShape = RoundedCornerShape(0)

private val DefaultLightShadowColor = Color.White.copy(0.8f)
private val DefaultDarkShadowColor = Color(0xFFA6B4C8).copy(0.7f)

private val DefaultSpotLight = SpotLight.TOP_LEFT

@Composable
fun Neumorphic(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = DefaultNeumorphicShape,
    color: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    spotLight: SpotLight = DefaultSpotLight,
    lightShadowColor: Color = Material.colors.lightShadowColor,
    darkShadowColor: Color = Material.colors.darkShadowColor,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
    ) {
        Box(
            modifier = modifier
                .shadow(
                    shape = shape,
                    border = border,
                    elevation = elevation,
                    spotLight = spotLight,
                    lightShadowColor = lightShadowColor,
                    darkShadowColor = darkShadowColor
                )
                .background(color)
                .semantics(mergeDescendants = false) {}
                .pointerInput(Unit) {},
            propagateMinConstraints = true,
            //       contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun Neumorphic(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: CornerBasedShape = DefaultNeumorphicShape,
    color: Color = Material.colors.background,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    spotLight: SpotLight = DefaultSpotLight,
    lightShadowColor: Color = Material.colors.lightShadowColor,
    darkShadowColor: Color = Material.colors.darkShadowColor,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor
    ) {
        Box(
            modifier = modifier
                //.minimumTouchTargetSize()
                .shadow(
                    shape = shape,
                    border = border,
                    elevation = elevation,
                    spotLight = spotLight,
                    lightShadowColor = lightShadowColor,
                    darkShadowColor = darkShadowColor
                )
                .background(color)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                ),
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun Neumorphic(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: CornerBasedShape = DefaultNeumorphicShape,
    color: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    spotLight: SpotLight = DefaultSpotLight,
    lightShadowColor: Color = Material.colors.lightShadowColor,
    darkShadowColor: Color = Material.colors.darkShadowColor,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
    ) {
        Box(
            modifier = modifier
                //.minimumTouchTargetSize()
                .shadow(
                    shape = shape,
                    border = border,
                    elevation = elevation,
                    spotLight = spotLight,
                    lightShadowColor = lightShadowColor,
                    darkShadowColor = darkShadowColor
                )
                .background(color)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Tab,
                    onClick = onClick
                ),
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}


@ExperimentalMaterialApi
@Composable
fun Neumorphic(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: CornerBasedShape = DefaultNeumorphicShape,
    color: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    spotLight: SpotLight = DefaultSpotLight,
    lightShadowColor: Color = Material.colors.lightShadowColor,
    darkShadowColor: Color = Material.colors.darkShadowColor,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
    ) {
        Box(
            modifier = modifier
                //.minimumTouchTargetSize()
                .shadow(
                    shape = shape,
                    border = border,
                    elevation = elevation,
                    spotLight = spotLight,
                    lightShadowColor = lightShadowColor,
                    darkShadowColor = darkShadowColor
                )
                .background(color)
                .toggleable(
                    value = checked,
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                ),
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}