@file:Suppress("TransitionPropertiesLabel")

package com.prime.media.core.compose.menu


import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MenuDefaults
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.primex.material2.Text

private const val TAG = "DropDownMenu"

// Size defaults.
private val MenuElevation = 8.dp
private val DropdownMenuItemHorizontalPadding = 16.dp
internal val DropdownMenuVerticalPadding = 8.dp
private val DropdownMenuItemDefaultMinWidth = 112.dp
private val DropdownMenuItemDefaultMaxWidth = 280.dp
private val DropdownMenuItemDefaultMinHeight = 48.dp

// Menu open/close animation.
internal const val InTransitionDuration = 120
internal const val OutTransitionDuration = 75

/**
 * Represents the surface of the [Popup2] Menu.
 */
@Composable
private fun Surface(
    expandedStates: MutableTransitionState<Boolean>,
    transformOriginState: MutableState<TransformOrigin>,
    modifier: Modifier = Modifier,
    elevation: Dp = Dp.Unspecified,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.onSurface,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    // Menu open/close animation.
    val transition = updateTransition(expandedStates, "DropDownMenu")

    val scale by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(
                    durationMillis = InTransitionDuration,
                    easing = LinearOutSlowInEasing
                )
            } else {
                // Expanded to dismissed.
                tween(
                    durationMillis = 1,
                    delayMillis = OutTransitionDuration - 1
                )
            }
        }
    ) {
        if (it) {
            // Menu is expanded.
            1f
        } else {
            // Menu is dismissed.
            0.8f
        }
    }

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(durationMillis = 30)
            } else {
                // Expanded to dismissed.
                tween(durationMillis = OutTransitionDuration)
            }
        }
    ) {
        if (it) {
            // Menu is expanded.
            1f
        } else {
            // Menu is dismissed.
            0f
        }
    }
    Card(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
            transformOrigin = transformOriginState.value
        },
        elevation = elevation.takeOrElse { MenuElevation },
        contentColor = contentColor,
        content = content,
        backgroundColor = backgroundColor,
        border = border,
        shape = shape
    )
}

/**
 * A composable function that creates a customizable popup window with various appearance and behavior options.
 * The popup window can serve as a dropdown menu, tooltip, dialog, or any other overlay component.
 * It automatically animates when shown or dismissed and can be closed by clicking outside or pressing the back button.
 *
 * @param expanded A boolean state controlling the visibility of the popup window. When true, the window is visible; when false, it's hidden.
 * @param onDismissRequest A callback invoked when the user attempts to dismiss the popup. This can occur when clicking outside, pressing the back button, or manually calling [PopupScope.dismiss].
 * @param offset A [DpOffset] specifying the popup's position relative to its parent. Default is (0.dp, 0.dp), aligning the top-left corner with its parent.
 * @param elevation A [Dp] specifying the popup's elevation, affecting its shadow. Default is [Dp.Unspecified], using [PopupDefaults.elevation].
 * @param backgroundColor A [Color] setting the popup's background color. Default is [MaterialTheme.colors.surface], adapting to the current theme.
 * @param contentColor A [Color] setting the content color of the popup window.
 * @param shape A [Shape] defining the popup's shape. Default is [MaterialTheme.shapes.small].
 * @param border A [BorderStroke] specifying the border appearance of the popup.
 * @param properties Additional [PopupProperties] to customize the popup's behavior, with focusability enabled by default.
 * @param content The content of the popup, defined as a composable lambda.
 *
 * Example usage:
 * ```kotlin
 * Popup2(
 *     expanded = isPopupVisible,
 *     onDismissRequest = { isPopupVisible = false },
 *     offset = DpOffset(8.dp, 16.dp),
 *     elevation = 4.dp,
 *     backgroundColor = Color.White,
 *     contentColor = Color.Black,
 *     shape = RoundedCornerShape(8.dp),
 *     border = BorderStroke(1.dp, Color.Gray),
 *     properties = PopupProperties(focusable = true),
 * ) {
 *     // Content of the popup
 *     Text("Hello, Popup!")
 * }
 * ```
 */
@Composable
fun Popup2(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    elevation: Dp = Dp.Unspecified,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.onSurface,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable () -> Unit
){
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded

    if (expandedStates.currentState || expandedStates.targetState) {
        val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
        val density = LocalDensity.current
        val popupPositionProvider = DropdownMenuPositionProvider(
            offset,
            density
        ) { parentBounds, menuBounds ->
            transformOriginState.value = calculateTransformOrigin(parentBounds, menuBounds)
        }

        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider,
            properties = properties
        ) {
            Surface(
                expandedStates = expandedStates,
                transformOriginState = transformOriginState,
                elevation = elevation,
                shape = shape,
                content = content,
                contentColor = contentColor,
                backgroundColor = backgroundColor,
                border = border
            )
        }
    }
}

/**
 * @see Popup2
 * @see DropdownMenu
 */
@Composable
@NonRestartableComposable
fun DropdownMenu2(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    elevation: Dp = Dp.Unspecified,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = MaterialTheme.colors.onSurface,
    shape: Shape = MaterialTheme.shapes.small,
    border: BorderStroke? = null,
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    Popup2(
        expanded,
        onDismissRequest,
        offset,
        elevation,
        backgroundColor,
        contentColor,
        shape,
        border,
        properties
    ){
        Column(
            modifier = Modifier
                .padding(vertical = DropdownMenuVerticalPadding)
                .width(IntrinsicSize.Max)
                .verticalScroll(scrollState),
            content = content
        )
    }
}


/**
 * An 2nd variant of DropDownMenuItem
 */
@Composable
fun DropdownMenuItem2(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    // TODO(popam, b/156911853): investigate replacing this Row with ListItem
    Row(
        modifier = modifier
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = rememberRipple(true)
            )
            .fillMaxWidth()
            // Preferred min and max width used during the intrinsic measurement.
            .sizeIn(
                minWidth = DropdownMenuItemDefaultMinWidth,
                maxWidth = DropdownMenuItemDefaultMaxWidth,
                minHeight = DropdownMenuItemDefaultMinHeight
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val typography = MaterialTheme.typography
        ProvideTextStyle(typography.body2) {
            val contentAlpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled
            CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
                content()
            }
        }
    }
}

/**
 * @see DropDownMenuItem2
 */
@Composable
inline fun DropdownMenuItem2(
    title: CharSequence,
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: Painter? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    DropdownMenuItem2(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        if (leading != null)
            Icon(
                painter = leading,
                contentDescription = null,
                //  modifier = Modifier.padding(start = 16.dp)
            )

        // the text
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp),
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}