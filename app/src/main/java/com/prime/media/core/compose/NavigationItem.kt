
package com.prime.media.core.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigationDefaults
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.SelectableChipColors
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.primex.material2.Label

private const val TAG = "NavigationItem"

/**
 * The color opacity used for a selected chip's leading icon overlay.
 */
private const val SelectedOverlayOpacity = 0.16f

/**
 * A utility function for defining colors used by both [NavigationRailItem2] and [BottomNavItem2].
 *
 * @param contentColor The color for the content of the item when unselected.
 * @param leadingIconColor The color for the leading icon of the item when unselected.
 * @param disabledLeadingIconColor The color for the leading icon when the item is disabled.
 * @param selectedContentColor The color for the content of the item when selected.
 * @param selectedLeadingIconColor The color for the leading icon of the item when selected.
 *
 * @return [SelectableChipColors] object with the specified color configurations.
 */
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal inline fun BottomNavigationDefaults.navigationItem2Colors(
    contentColor: Color = MaterialTheme.colors.onSurface.copy(ChipDefaults.ContentOpacity),
    leadingIconColor: Color = contentColor.copy(ChipDefaults.LeadingIconOpacity),
    disabledLeadingIconColor: Color = leadingIconColor.copy(alpha = ContentAlpha.disabled * ChipDefaults.LeadingIconOpacity),
    selectedContentColor: Color = MaterialTheme.colors.onSurface.copy(alpha = SelectedOverlayOpacity).compositeOver(contentColor),
    selectedLeadingIconColor: Color = MaterialTheme.colors.onSurface.copy(alpha = SelectedOverlayOpacity).compositeOver(leadingIconColor)
): SelectableChipColors = ChipDefaults.outlinedFilterChipColors(
    contentColor = contentColor,
    disabledLeadingIconColor = disabledLeadingIconColor,
    selectedContentColor = selectedContentColor,
    selectedLeadingIconColor = selectedLeadingIconColor,
    backgroundColor = Color.Transparent,
    selectedBackgroundColor = Color.Transparent,
    disabledBackgroundColor = Color.Transparent,
)

/**
 * The outline border that is to be used with the [BottomNavigationItem2]
 */
@OptIn(ExperimentalMaterialApi::class)
inline val BottomNavigationDefaults.outlinedBorder @Composable get() = ChipDefaults.outlinedBorder

/**
 * The content padding used by a Item.
 * Used as start padding when there's leading icon, used as eng padding when there's no
 * trailing icon.
 */
private val CheckedPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
private val UnCheckedPadding = PaddingValues(10.dp)

/**
* The size of the spacing between the leading icon and a text inside a chip.
*/
private val LeadingIconSpacing = 10.dp

/**
 * Represents a different style of BottomNavigationItem. This behaves similarly to the Material2 [FilterChip].
 *
 * @param onClick Callback to be invoked when the item is clicked.
 * @param icon Composable function to define the icon displayed in the item.
 * @param label Composable function to define the label displayed in the item.
 * @param modifier Modifier for styling and positioning the item.
 * @param checked Whether the item is currently selected or not.
 * @param enabled Whether the item is interactable or not.
 * @param shape The shape of the item.
 * @param border The border configuration for the item, visible only when checked.
 * @param colors Customizable colors for the item, including background and content colors.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomNavigationItem2(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    border: BorderStroke = ChipDefaults.outlinedBorder,
    colors: SelectableChipColors = ChipDefaults.outlinedFilterChipColors(backgroundColor = Color.Transparent)
) {
    // This item necessitates the presence of both an icon and a label.
    // When selected, both the icon and label are displayed; otherwise, only the icon is shown.
    // The border is visible only when the item is selected; otherwise, it remains hidden.
    // The background color of this item is set to Color.Transparent.
    // TODO(b/113855296): Animate transition between unselected and selected
    val contentColor by colors.contentColor(enabled, checked)
    Surface(
        selected = checked,
        onClick = onClick,
        shape = shape,
        modifier = modifier.scale(0.87f),
        color = Color.Transparent,
        contentColor = contentColor,
        border = if (checked) border else null,
    ) {
        Row(
            Modifier
                .width(IntrinsicSize.Max)
                .defaultMinSize(minHeight = ChipDefaults.MinHeight)
                .padding(if (checked) CheckedPadding else UnCheckedPadding)
                .animateContentSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leadingIconColor = colors.leadingIconColor(enabled, checked)
            CompositionLocalProvider(
                LocalContentColor provides leadingIconColor.value,
                LocalContentAlpha provides leadingIconColor.value.alpha,
                content = icon
            )
            if (!checked) return@Row
            Spacer(Modifier.width(LeadingIconSpacing))
            CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.caption,
                    label
                )
            }
        }
    }
}

/**
 * @see BottomNavigationItem2
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
inline fun BottomNavigationItem2(
    noinline onClick: () -> Unit,
    icon: ImageVector,
    label: CharSequence,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    border: BorderStroke = ChipDefaults.outlinedBorder,
    colors: SelectableChipColors = ChipDefaults.outlinedFilterChipColors(backgroundColor = Color.Transparent)
)= BottomNavigationItem2(
    onClick = onClick,
    icon = {
        Icon(
            imageVector = icon,
            contentDescription = null
        )
    },
    label = { Label(text = label) },
    modifier = modifier,
    checked = checked,
    enabled = enabled,
    shape = shape,
    border = border,
    colors = colors
)


/**
 * The default shape of the [NavigationRailItem2]
 */
private val DefaultNavRailItemShape = RoundedCornerShape(20)

/**
 * @see BottomNavigationItem2
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NavigationRailItem2 (
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = DefaultNavRailItemShape,
    border: BorderStroke = ChipDefaults.outlinedBorder,
    colors: SelectableChipColors = ChipDefaults.outlinedFilterChipColors(backgroundColor = Color.Transparent)
) {
    // This item necessitates the presence of both an icon and a label.
    // When selected, both the icon and label are displayed; otherwise, only the icon is shown.
    // The border is visible only when the item is selected; otherwise, it remains hidden.
    // The background color of this item is set to Color.Transparent.
    // TODO(b/113855296): Animate transition between unselected and selected
    val contentColor by colors.contentColor(enabled, checked)
    Surface(
        selected = checked,
        onClick = onClick,
        shape = if (checked) shape else CircleShape,
        modifier = modifier.scale(0.87f).animateContentSize(),
        color = Color.Transparent,
        contentColor = contentColor,
        border = if (checked) border else null,
    ) {
        Column(
            Modifier
                .height(IntrinsicSize.Max)
                .defaultMinSize(minHeight = ChipDefaults.MinHeight)
                .padding(if (checked) CheckedPadding else UnCheckedPadding)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val leadingIconColor = colors.leadingIconColor(enabled, checked)
            CompositionLocalProvider(
                LocalContentColor provides leadingIconColor.value,
                LocalContentAlpha provides leadingIconColor.value.alpha,
                content = icon
            )
            if (!checked) return@Column
            Spacer(Modifier.height(LeadingIconSpacing))
            CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.caption,
                    label
                )
            }
        }
    }
}

/**
 * @see BottomNavigationItem2
 */
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
@OptIn(ExperimentalMaterialApi::class)
@Composable
inline fun NavigationRailItem2 (
    noinline onClick: () -> Unit,
    icon: ImageVector,
    label: CharSequence,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = DefaultNavRailItemShape,
    border: BorderStroke = ChipDefaults.outlinedBorder,
    colors: SelectableChipColors = ChipDefaults.outlinedFilterChipColors(backgroundColor = Color.Transparent)
)= NavigationRailItem2(
    onClick = onClick,
    icon = {
        Icon(
            imageVector = icon,
            contentDescription = null
        )
    },
    label = { Label(text = label) },
    modifier = modifier,
    checked = checked,
    enabled = enabled,
    shape = shape,
    border = border,
    colors = colors
)