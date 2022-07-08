package com.prime.player.common.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prime.player.Material
import com.prime.player.darkShadowColor
import com.prime.player.lightShadowColor
import com.primex.core.shadow.SpotLight
import com.primex.ui.Label

/**
 * Represents the background and content colors used in a button in different states.
 *
 * See [ButtonDefaults.buttonColors] for the default colors used in a [ButtonDefaults].
 * See [ButtonDefaults.outlinedButtonColors] for the default colors used in a
 * [OutlinedButton].
 * See [ButtonDefaults.textButtonColors] for the default colors used in a [TextButton].
 */
@Stable
interface NeumorphicButtonColors {
    /**
     * Represents the background color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    fun backgroundColor(enabled: Boolean): State<Color>

    /**
     * Represents the content color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    fun contentColor(enabled: Boolean): State<Color>

    /**
     * Represents the color of the shadow for the button. Depends on the [elevation] and [adjacent].
     * @param elevation the elevation of the button. might be negative. returned shadow color depends
     * on the value of elevation is positive or negative.
     * @param adjacent: true return the value adjacent to the light source else opposite.
     */
    fun shadow(elevation: Dp, adjacent: Boolean): Color
}


/**
 * Default [NeumorphicButtonColors] implementation.
 */
@Immutable
private class DefaultNeumorphicButtonColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val disabledBackgroundColor: Color,
    private val disabledContentColor: Color,
    private val lightShadowColor: Color,
    private val darkShadowColor: Color
) : NeumorphicButtonColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) backgroundColor else disabledBackgroundColor)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
    }

    override fun shadow(elevation: Dp, adjacent: Boolean): Color {
        return when {
            elevation > 0.dp && adjacent -> lightShadowColor
            // opposite
            elevation > 0.dp && !adjacent -> darkShadowColor
            elevation < 0.dp && adjacent -> darkShadowColor

            // case elevation < 0.dp && !adjacent
            else -> lightShadowColor
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultNeumorphicButtonColors

        if (backgroundColor != other.backgroundColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledBackgroundColor != other.disabledBackgroundColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (lightShadowColor != other.lightShadowColor) return false
        if (darkShadowColor != other.darkShadowColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + lightShadowColor.hashCode()
        result = 31 * result + darkShadowColor.hashCode()
        return result
    }
}

object NeumorphicButtonDefaults {

    private val ButtonHorizontalPadding = 16.dp
    private val ButtonVerticalPadding = 8.dp

    /**
     * The default content padding used by [ButtonDefaults]
     */
    val ContentPadding = PaddingValues(
        start = ButtonHorizontalPadding,
        top = ButtonVerticalPadding,
        end = ButtonHorizontalPadding,
        bottom = ButtonVerticalPadding
    )

    /**
     * The default min width applied for the [ButtonDefaults].
     * Note that you can override it by applying Modifier.widthIn directly on [ButtonDefaults].
     */
    val MinWidth = 64.dp

    /**
     * The default min height applied for the [ButtonDefaults].
     * Note that you can override it by applying Modifier.heightIn directly on [ButtonDefaults].
     */
    val MinHeight = 36.dp

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [ButtonDefaults].
     *
     * @param defaultElevation the elevation to use when the [ButtonDefaults] is enabled, and has no
     * other [Interaction]s.
     * @param pressedElevation the elevation to use when the [ButtonDefaults] is enabled and
     * is pressed.
     * @param disabledElevation the elevation to use when the [ButtonDefaults] is not enabled.
     * @param hoveredElevation the elevation to use when the [ButtonDefaults] is enabled and is hovered.
     * @param focusedElevation the elevation to use when the [ButtonDefaults] is enabled and is focused.
     */
    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun elevation(
        defaultElevation: Dp = 6.dp,
        pressedElevation: Dp = 0.dp,
        disabledElevation: Dp = 0.dp,
        hoveredElevation: Dp = 7.dp,
        focusedElevation: Dp = 7.dp,
    ) =
        ButtonDefaults.elevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            disabledElevation = disabledElevation,
            hoveredElevation = hoveredElevation,
            focusedElevation = focusedElevation
        )

    /**
     * Creates a [NeumorphicButtonColors] that represents the default background and content colors used in
     * a [NeumorphicButton].
     *
     * @param backgroundColor the background color of this [NeumorphicButton] when enabled
     * @param contentColor the content color of this [NeumorphicButton] when enabled
     * @param disabledBackgroundColor the background color of this [NeumorphicButton] when not enabled
     * @param disabledContentColor the content color of this [NeumorphicButton] when not enabled
     */
    @Composable
    fun neumorphicButtonColors(
        backgroundColor: Color = MaterialTheme.colors.background,
        contentColor: Color = contentColorFor(backgroundColor),
        disabledBackgroundColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.12f)
            .compositeOver(MaterialTheme.colors.background),
        disabledContentColor: Color = MaterialTheme.colors.onBackground
            .copy(alpha = ContentAlpha.disabled),
        lightShadowColor: Color = Material.colors.lightShadowColor,
        darkShadowColor: Color = Material.colors.darkShadowColor
    ): NeumorphicButtonColors =
        DefaultNeumorphicButtonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            disabledBackgroundColor = disabledBackgroundColor,
            disabledContentColor = disabledContentColor,
            lightShadowColor = lightShadowColor,
            darkShadowColor = darkShadowColor
        )
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = MaterialTheme.shapes.small,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation = NeumorphicButtonDefaults.elevation(),
    border: BorderStroke? = null,
    colors: NeumorphicButtonColors = NeumorphicButtonDefaults.neumorphicButtonColors(),
    contentPadding: PaddingValues = NeumorphicButtonDefaults.ContentPadding,
    spotLight: SpotLight = SpotLight.TOP_LEFT,
    content: @Composable RowScope.() -> Unit,
) {
    val contentColor by colors.contentColor(enabled)
    val depth by elevation.elevation(enabled = enabled, interactionSource = interactionSource)

    Neumorphic(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = colors.backgroundColor(enabled).value,
        contentColor = contentColor.copy(alpha = 1f),
        border = border,
        elevation = depth,
        interactionSource = interactionSource,
        lightShadowColor = colors.shadow(elevation = depth, true),
        darkShadowColor = colors.shadow(elevation = depth, false),
        spotLight = spotLight
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(
                value = MaterialTheme.typography.button
            ) {
                Row(
                    Modifier
                        .defaultMinSize(
                            minWidth = NeumorphicButtonDefaults.MinWidth,
                            minHeight = NeumorphicButtonDefaults.MinHeight
                        )
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}