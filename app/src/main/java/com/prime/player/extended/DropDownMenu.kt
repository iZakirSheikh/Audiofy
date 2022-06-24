package com.prime.player.extended

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val DropdownMenuItemDefaultMinWidth = 175.dp
private val DropdownMenuItemDefaultMaxWidth = 320.dp
private val DropdownMenuItemDefaultMinHeight = 48.dp
private val defaultSizeModifier = Modifier
    .fillMaxWidth()
    // Preferred min and max width used during the intrinsic measurement.
    .sizeIn(
        minWidth = DropdownMenuItemDefaultMinWidth,
        maxWidth = DropdownMenuItemDefaultMaxWidth,
        minHeight = DropdownMenuItemDefaultMinHeight
    )

@JvmName("DropdownMenu1")
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DropdownMenu(
    title: String? = null,
    checked: Int = -1,
    isEnabled: ((index: Int) -> Boolean)? = null,
    expanded: Boolean,
    items: List<String>,
    onDismissRequest: () -> Unit,
    onItemClick: (index: Int) -> Unit,
) {
    DropdownMenu(
        title = title,
        preserveIconSpace = false,
        checked = checked,
        isEnabled = isEnabled,
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        onItemClick = onItemClick,
        items = items.map {
            null to it
        },
    )
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DropdownMenu(
    title: String? = null,
    preserveIconSpace: Boolean = false,
    checked: Int = -1,
    isEnabled: ((index: Int) -> Boolean)? = null,
    expanded: Boolean,
    items: List<Pair<ImageVector?, String>>,
    onDismissRequest: () -> Unit,
    onItemClick: (index: Int) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        // The Menu Title
        title?.let {
            DropdownMenuItem(onClick = { /*Do Nothing*/ }, enabled = false) {
                Label(
                    text = it,
                    modifier = Modifier.padding(Padding.MEDIUM),
                    fontWeight = FontWeight.Bold,
                )
            }
            Divider()
        }

        items.forEachIndexed { index, pair ->
            val enabled = isEnabled?.invoke(index) ?: true
            val selected = checked == index

            val color = (if (selected) MaterialTheme.colors.primary else LocalContentColor.current)
                .copy(if (enabled) ContentAlpha.high else ContentAlpha.disabled)

            DropdownMenuItem(
                onClick = {
                    onItemClick(index)
                    onDismissRequest()
                },
                contentPadding = PaddingValues(0.dp),
                modifier = defaultSizeModifier
            ) {
                // indicator
                if (selected)
                    Spacer(
                        modifier = Modifier
                            .height(DropdownMenuItemDefaultMinHeight)
                            .requiredWidth(5.dp)
                            .background(color = color)
                    )

                val icon = pair.first
                val title = pair.second
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.padding(start = Padding.LARGE)
                    )
                }

                // the text
                Label(
                    text = title,
                    modifier = icon
                        .let {
                            if (it == null && preserveIconSpace)
                                Modifier.padding(start = 40.dp)
                            else
                                Modifier
                        }
                        .padding(horizontal = Padding.LARGE)

                        .weight(1f),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = color
                )
            }
        }
    }
}