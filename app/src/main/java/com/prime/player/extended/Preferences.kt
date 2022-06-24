package com.prime.player.extended


import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A General [Preference(title = )] representation.
 * The basic building block that represents an individual setting displayed to a user in the preference hierarchy.
 * @param modifier -> [Modifier] allows to modify the outer wrapper of this preference
 * @param enabled -> [Boolean]  Sets whether this preference should disable its view when it gets disabled.
 * @param singleLineTitle - [Boolean] Sets whether to constrain the title of this preference to a single line instead of letting it wrap onto multiple lines.
 * @param iconSpaceReserved - [Boolean] Sets whether to reserve the space of this preference icon view when no icon is provided. If set to true, the preference will be offset as if it would have the icon and thus aligned with other preferences having icons.
 * @param icon -[ImageVector] - Sets the icon for this preference with a [ImageVector].
 * @param summery - [String] - Sets the summary for this preference with a [String].
 * @param title - Sets the title for this preference with a [String].
 * @param widget - Sets the layout for the controllable widget portion of this preference.
 */
@Composable
fun Preference(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLineTitle: Boolean = true,
    iconSpaceReserved: Boolean = true,
    icon: ImageVector? = null,
    summery: String? = null,
    title: String,
    widget: @Composable() (BoxScope.() -> Unit)? = null
) {
    val color = if (enabled) LocalContentColor.current else LocalContentColor.current.copy(0.5f)
    CompositionLocalProvider(LocalContentColor provides color) {
        Preference(
            modifier = modifier,
            singleLineTitle = singleLineTitle,
            iconSpaceReserved = iconSpaceReserved,
            icon = icon,
            summery = summery,
            title = title,
            widget = widget
        )
    }
}

@Composable
private fun Preference(
    modifier: Modifier = Modifier,
    singleLineTitle: Boolean = true,
    iconSpaceReserved: Boolean = true,
    icon: ImageVector? = null,
    summery: String? = null,
    title: String,
    widget: @Composable() (BoxScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize()
            .then(modifier)
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crossfade(targetState = icon) { newIcon ->
            when {
                newIcon == null && iconSpaceReserved -> Spacer(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .requiredSize(24.dp)
                )
                newIcon != null -> Icon(
                    imageVector = newIcon,
                    contentDescription = "prefrence icon",
                    modifier = Modifier
                        //.requiredSize(24.dp)
                        .padding(start = 16.dp),
                    tint = LocalContentColor.current
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = if (icon == null) 16.dp else 0.dp,
                    top = 8.dp,
                    bottom = 8.dp
                )
                .weight(1f)
        ) {
            Crossfade(targetState = title) {
                Text(
                    text = it,
                    maxLines = if (singleLineTitle) 1 else 3,
                    style = MaterialTheme.typography.body1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold,
                    color = LocalContentColor.current
                )
            }

            Crossfade(targetState = summery) { newSummery ->
                if (!newSummery.isNullOrEmpty())
                    Text(
                        text = newSummery,
                        maxLines = 6,
                        style = MaterialTheme.typography.body2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .fillMaxWidth(),
                        color = LocalContentColor.current.copy(0.6f),
                        fontWeight = FontWeight.Normal
                    )
            }
        }
        if (widget != null)
            Box(
                modifier = Modifier
                    .padding(start = 8.dp, end = 16.dp),
                // .widthIn(max = 56.dp),
                content = widget
            )
    }
}

@Composable
private fun ExpandablePreference(
    modifier: Modifier = Modifier,
    singleLineTitle: Boolean = true,
    iconSpaceReserved: Boolean = true,
    icon: ImageVector? = null,
    summery: String? = null,
    title: String,
    enabled: Boolean = true,
    lockExpanded: Boolean = false,
    innerPref: Modifier? = null,
    widget: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source by androidx.compose.runtime.remember {
        lazy {
            MutableInteractionSource()
        }
    }

    val stateExpanded by if (lockExpanded) androidx.compose.runtime.remember {
        mutableStateOf(true)
    } else source.collectIsFocusedAsState()

    Column(
        modifier = if (lockExpanded) Modifier else Modifier
            .acquireFocusOnInteraction(source, indication = LocalIndication.current)
            .wrapContentSize()
            .then(modifier)
            .animateContentSize()
    ) {
        // Main Preference
        Preference(
            title = title,
            summery = summery,
            icon = icon,
            enabled = enabled,
            singleLineTitle = singleLineTitle,
            iconSpaceReserved = iconSpaceReserved,
            modifier = innerPref ?: Modifier,
            widget = widget
        )
        if (stateExpanded)
            content()
    }
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLineTitle: Boolean = true,
    iconSpaceReserved: Boolean = true,
    icon: ImageVector? = null,
    summery: String? = null,
    checked: Boolean,
    title: String,
    onCheckedChange: ((Boolean) -> Unit)
) {
    Preference(
        modifier = modifier.clickable(enabled = enabled) {
            onCheckedChange(!checked)
        },
        title = title,
        enabled = enabled,
        singleLineTitle = singleLineTitle,
        iconSpaceReserved = iconSpaceReserved,
        icon = icon,
        summery = summery
    ) {
        Switch(enabled = enabled, checked = checked, onCheckedChange = null)
    }
}

@Composable
fun CheckBoxPreference(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLineTitle: Boolean = true,
    iconSpaceReserved: Boolean = true,
    icon: ImageVector? = null,
    summery: String? = null,
    checked: Boolean,
    title: String,
    onCheckedChange: ((Boolean) -> Unit)
) {
    Preference(
        modifier = modifier.clickable(enabled = enabled) {
            onCheckedChange(!checked)
        },
        title = title,
        enabled = enabled,
        singleLineTitle = singleLineTitle,
        iconSpaceReserved = iconSpaceReserved,
        icon = icon,
        summery = summery
    ) {
        Checkbox(enabled = enabled, checked = checked, onCheckedChange = null)
    }
}

@Composable
fun <T> DropDownPreference(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLineTitle: Boolean = true,
    iconSpaceReserved: Boolean = true,
    icon: ImageVector? = null,
    title: String,
    entries: List<Pair<String, T>>,
    defaultValue: T,
    onRequestChange: (T) -> Unit
) {
    require(entries.isNotEmpty())

    var expanded by androidx.compose.runtime.remember {
        mutableStateOf(false)
    }

    val default = entries.find { (_, value) ->
        value == defaultValue
    }!!.first

    Preference(
        modifier = modifier.clickable(enabled = enabled) {
            expanded = true
        },
        title = title,
        enabled = enabled,
        singleLineTitle = singleLineTitle,
        iconSpaceReserved = iconSpaceReserved,
        icon = icon,
        summery = default
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { (placeHolder, value) ->
                DropdownMenuItem(
                    onClick = {
                        if (value != defaultValue) {
                            onRequestChange(value)
                            expanded = false
                        }
                    }
                ) {
                    RadioButton(
                        selected = value == defaultValue,
                        // = null,
                        enabled = enabled,
                        onClick = null
                    )

                    Text(
                        text = placeHolder,
                        style = MaterialTheme.typography.body1,
                        fontWeight = if (value != defaultValue) FontWeight.SemiBold else FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp)
                            .fillMaxSize(),
                        maxLines = 2,
                        color = if (value == defaultValue) MaterialTheme.colors.secondary else LocalContentColor.current
                    )
                }
            }
        }
    }
}


@Composable
fun ColorPickerPreference(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLineTitle: Boolean = true,
    iconSpaceReserved: Boolean = true,
    icon: ImageVector? = Icons.Default.ColorLens,
    summery: String? = null,
    title: String,
    lockExpanded: Boolean = false,
    defaultEntry: Color,
    entries: List<Color>,
    onRequestValueChange: (Color) -> Unit,
) {
    ExpandablePreference(
        modifier = modifier,
        title = title,
        enabled = enabled,
        singleLineTitle = singleLineTitle,
        iconSpaceReserved = iconSpaceReserved,
        icon = icon,
        lockExpanded = lockExpanded,
        summery = summery,
        widget = {
            val color by animateColorAsState(targetValue = defaultEntry)
            Spacer(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(color = color, shape = CircleShape)
                    .requiredSize(40.dp)
            )
        }
    ) {
        val checked = androidx.compose.runtime.remember {
            mutableStateOf(defaultEntry)
        }

        ColorPicker(
            entries = entries,
            checked = checked,
            modifier = Modifier.padding(
                start = 16.dp + if (iconSpaceReserved) 24.dp + 16.dp else 0.dp,
                top = 10.dp,
                bottom = 10.dp
            )
        )

        // controls


        Row(
            modifier = Modifier
                .padding(
                    start = 16.dp + if (iconSpaceReserved) 24.dp + 16.dp else 0.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                    // top = 16.dp
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val manager = LocalFocusManager.current
            TextButton(onClick = {
                if (!lockExpanded)
                    manager.clearFocus(true)
            }) {
                Text(
                    text = stringResource(id = android.R.string.cancel),
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(onClick = {
                if (!lockExpanded)
                    manager.clearFocus(true)
                onRequestValueChange(checked.value)
            }) {
                Text(
                    text = stringResource(id = android.R.string.ok),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}



