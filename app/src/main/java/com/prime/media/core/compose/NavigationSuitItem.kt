@file:OptIn(ExperimentalMaterialApi::class)

package com.prime.media.core.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
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
import androidx.compose.ui.unit.dp
import com.prime.media.Material

private val DefaultNavItemShape = RoundedCornerShape(20)

object NavigationItemDefaults {
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun navigationItemColors(
        backgroundColor: Color = Color.Transparent,
        contentColor: Color = LocalContentColor.current,
        selectedBackgroundColor: Color = Material.colors.primary.copy(0.08f),
        selectedContentColor: Color = Material.colors.primary,
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
private val DefaultIconSpacing = 4.dp

/**
 * The content padding used by a Item when it's unchecked.
 */
private val UnCheckedPadding = PaddingValues(10.dp)

@Composable
fun BottomNavItem(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = DefaultNavItemShape,
    colors: SelectableChipColors = NavigationItemDefaults.navigationItemColors()
) {
    val contentColor by colors.contentColor(enabled, checked)
    val backgroundColor by colors.backgroundColor(enabled = enabled, selected = checked)
    Surface(
        onClick = onClick,
        shape = shape,
        selected = checked,
        enabled = enabled,
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier.animateContentSize(alignment = Alignment.CenterStart)
    ) {
        Row(
            Modifier
                .wrapContentSize()
                .padding(if (checked) CheckedPadding else UnCheckedPadding)
                .animateContentSize(),
            horizontalArrangement = if (checked) Arrangement.spacedBy(DefaultIconSpacing) else Arrangement.Center,
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
                    value = Material.typography.caption,
                    label
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NavRailItem(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = DefaultNavItemShape,
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
        modifier = modifier.animateContentSize(alignment = Alignment.TopCenter),
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        Column(
            Modifier.padding(UnCheckedPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (!checked) Arrangement.spacedBy(DefaultIconSpacing) else Arrangement.Center,
        ) {
            val leadingIconColor = colors.leadingIconColor(enabled, checked)
            CompositionLocalProvider(
                LocalContentColor provides leadingIconColor.value,
                LocalContentAlpha provides leadingIconColor.value.alpha,
                content = icon
            )
            // Label is only shown when checked.
            // return from here if not checked.
            if (checked) return@Column
            CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
                ProvideTextStyle(
                    value = Material.typography.overline,
                    label
                )
            }
        }
    }
}
