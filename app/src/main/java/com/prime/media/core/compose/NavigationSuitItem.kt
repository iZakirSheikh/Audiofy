package com.prime.media.core.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private const val TAG = "NavigationItem"

object NavigationItemDefaults {

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun navigationItemColors(
        backgroundColor: Color = Color.Transparent,
        contentColor: Color = LocalContentColor.current,
        selectedBackgroundColor: Color = MaterialTheme.colors.primary.copy(0.08f),
        selectedContentColor: Color = MaterialTheme.colors.primary,
        disabledBackgroundColor: Color = Color.Transparent,
        disabledContentColor: Color = contentColor.copy(ContentAlpha.disabled),
    ) = ChipDefaults.outlinedFilterChipColors(
        backgroundColor = backgroundColor,
        leadingIconColor = contentColor,
        disabledContentColor = disabledContentColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledLeadingIconColor = disabledContentColor,
        selectedBackgroundColor = selectedBackgroundColor,
        selectedContentColor = selectedContentColor,
        selectedLeadingIconColor = selectedContentColor
    )
}

/**
 * A padding value used for checked components.
 */
private val CheckedPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/**
 * The size of the spacing between the leading icon and a text inside a chip.
 */
private val LeadingIconSpacing = 7.dp

/**
 * The content padding used by a Item when it's unchecked.
 */
private val UnCheckedPadding = PaddingValues(10.dp)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NavigationBarItem(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = CircleShape,
    border: BorderStroke? = null,
    colors: SelectableChipColors = NavigationItemDefaults.navigationItemColors()
) {
    // This item necessitates the presence of both an icon and a label.
    // When selected, both the icon and label are displayed; otherwise, only the icon is shown.
    // The border is visible only when the item is selected; otherwise, it remains hidden.
    // The background color of this item is set to Color.Transparent.
    // TODO(b/113855296): Animate transition between unselected and selected
    val contentColor by colors.contentColor(enabled, checked)
    val backgroundColor by colors.backgroundColor(enabled = enabled, selected = checked)
    Surface(
        selected = checked,
        onClick = onClick,
        shape = shape,
        modifier = modifier,
        color = backgroundColor,
        contentColor = contentColor,
        border = if (checked) border else null,
    ) {
        Row(
            Modifier
                .defaultMinSize(minHeight = ChipDefaults.MinHeight)
                .wrapContentSize()
                .padding(if (checked) CheckedPadding else UnCheckedPadding)
                .animateContentSize(),
            horizontalArrangement = if (checked) Arrangement.spacedBy(LeadingIconSpacing) else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val leadingIconColor = colors.leadingIconColor(enabled, checked)
            CompositionLocalProvider(
                LocalContentColor provides leadingIconColor.value,
                LocalContentAlpha provides leadingIconColor.value.alpha,
                content = icon
            )
            // if not checked return else show label.
            if (!checked) return@Row
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
 * The default shape of the [NavigationRailItem2]
 */
private val DefaultNavRailItemShape = RoundedCornerShape(20)

/**
 * A Composable function that represents a navigation rail item.
 *
 * @param onClick The callback to be invoked when the item is clicked.
 * @param icon The icon to be displayed for the item.
 * @param label The label to be displayed for the item.
 * @param modifier The modifier to be applied to the item.
 * @param checked Whether the item is currently checked.
 * @param enabled Whether the item is currently enabled.
 * @param shape The shape of the item.
 * @param border The border of the item.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NavigationRailItem(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = DefaultNavRailItemShape,
    border: BorderStroke? = null,
    colors: SelectableChipColors = NavigationItemDefaults.navigationItemColors()
) {
    // This item necessitates the presence of both an icon and a label.
    // When selected, both the icon and label are displayed; otherwise, only the icon is shown.
    // The border is visible only when the item is selected; otherwise, it remains hidden.
    // The background color of this item is set to Color.Transparent.
    // TODO(b/113855296): Animate transition between unselected and selected
    val contentColor by colors.contentColor(enabled, checked)
    val backgroundColor by colors.backgroundColor(enabled = enabled, selected = checked)
    Surface(
        selected = checked,
        onClick = onClick,
        shape = if (checked) shape else CircleShape,
        modifier = modifier.animateContentSize(),
        color = backgroundColor,
        contentColor = contentColor,
        border = if (checked) border else null,
    ) {
        Column(
            Modifier
                .defaultMinSize(
                    minHeight = ChipDefaults.MinHeight,
                    minWidth = ChipDefaults.MinHeight
                )
                .padding(if (checked) CheckedPadding else UnCheckedPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (checked) Arrangement.spacedBy(LeadingIconSpacing) else Arrangement.Center,
        ) {
            val leadingIconColor = colors.leadingIconColor(enabled, checked)
            CompositionLocalProvider(
                LocalContentColor provides leadingIconColor.value,
                LocalContentAlpha provides leadingIconColor.value.alpha,
                content = icon
            )
            // Label is only shown when checked.
            // return from here if not checked.
            if (!checked) return@Column
            CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.caption,
                    label
                )
            }
        }
    }
}

private val ActiveIndicatorHeight = 48.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NavigationDrawerItem(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    colors: SelectableChipColors = NavigationItemDefaults.navigationItemColors()
) {
    // This item necessitates the presence of both an icon and a label.
    // When selected, both the icon and label are displayed; otherwise, only the icon is shown.
    // The border is visible only when the item is selected; otherwise, it remains hidden.
    // The background color of this item is set to Color.Transparent.
    // TODO(b/113855296): Animate transition between unselected and selected
    val contentColor by colors.contentColor(enabled, checked)
    val backgroundColor by colors.backgroundColor(enabled = enabled, selected = checked)
    Surface(
        selected = checked,
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.Tab }
            .height(ActiveIndicatorHeight)
            .fillMaxWidth(),
        shape = shape,
        color = backgroundColor,
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LeadingIconSpacing),
        ) {
            val leadingIconColor = colors.leadingIconColor(enabled, checked)
            CompositionLocalProvider(
                LocalContentColor provides leadingIconColor.value,
                LocalContentAlpha provides leadingIconColor.value.alpha,
                content = icon
            )
            CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.body2,
                    label
                )
            }
        }
    }
}
