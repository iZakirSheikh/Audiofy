package com.prime.player.extended

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prime.player.audio.resolveAccentColor
import kotlinx.coroutines.delay


@Composable
fun BaseDialog(
    title: String,
    subtitle: String? = null,
    vectorIcon: ImageVector? = null,
    properties: DialogProperties = DialogProperties(),
    topBarBackgroundColor: Color = resolveAccentColor(),
    topBarContentColor: Color = suggestContentColorFor(backgroundColor = topBarBackgroundColor),
    topBarElevation: Dp = 0.dp,
    actions: @Composable RowScope.() -> Unit = {},
    button1: @Composable (() -> Unit)? = null,
    button2: @Composable (() -> Unit)? = null,
    bottom: @Composable (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Frame(
            modifier = Modifier
                .heightIn(min = 100.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colors.surface
        ) {
            Column {
                TopAppBar(
                    title = {
                        Header(
                            text = title,
                            secondaryText = subtitle,
                            style = MaterialTheme.typography.body1,
                        )
                    },
                    backgroundColor = topBarBackgroundColor,
                    contentColor = topBarContentColor,
                    elevation = topBarElevation,
                    navigationIcon = vectorIcon?.let {
                        @Composable {
                            Icon(
                                imageVector = it,
                                contentDescription = "dialog icon",
                                modifier = Modifier.padding(start = Padding.MEDIUM)
                            )
                        }
                    },
                    actions = actions
                )

                // the actual content
                content()

                // controls
                if (button1 != null || button2 != null)
                    Row(
                        modifier = Modifier
                            .padding(
                                bottom = Padding.MEDIUM,
                                end = Padding.MEDIUM,
                                start = Padding.MEDIUM
                            )
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        button1?.invoke()
                        button2?.invoke()
                    }
                // the bottom bar.
                bottom?.invoke()
            }
        }
    }
}

@Composable
fun PrimeDialog(
    title: String,
    subtitle: String? = null,
    vectorIcon: ImageVector? = null,
    properties: DialogProperties = DialogProperties(),
    bottom: @Composable (() -> Unit)? = null,
    topBarBackgroundColor: Color = resolveAccentColor(),
    topBarContentColor: Color = suggestContentColorFor(backgroundColor = topBarBackgroundColor),
    topBarElevation: Dp = 0.dp,
    imageButton: Pair<ImageVector, () -> Unit>? = null,
    button1: Pair<String, () -> Unit>? = null,
    button2: Pair<String, () -> Unit>? = null,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    BaseDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        subtitle = subtitle,
        vectorIcon = vectorIcon,
        content = content,
        properties = properties,
        topBarBackgroundColor = topBarBackgroundColor,
        topBarContentColor = topBarContentColor,
        topBarElevation = topBarElevation,
        bottom = bottom,
        actions = {
            imageButton?.let {
                IconButton(onClick = it.second) {
                    Icon(imageVector = it.first, contentDescription = null)
                }
            }
        },
        button1 = button1?.let {
            @Composable {
                TextButton(onClick = it.second, modifier = defaultSizeModifier) {
                    Label(
                        text = it.first,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        button2 = button2?.let {
            @Composable {
                TextButton(onClick = it.second, modifier = defaultSizeModifier) {
                    Label(
                        text = it.first,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
    )
}


private val defaultSizeModifier = Modifier
    // Preferred min and max width used during the intrinsic measurement.
    .sizeIn(
        maxWidth = 130.dp,
    )


@Composable
fun AlertDialog(
    title: String,
    message: String,
    vectorIcon: ImageVector? = null,
    onDismissRequest: (Boolean) -> Unit
) {
    PrimeDialog(
        title = title,
        vectorIcon = vectorIcon,
        onDismissRequest = { onDismissRequest(false) },
        button2 = "Confirm" to { onDismissRequest(true) },
        button1 = "Dismiss" to { onDismissRequest(false) }) {
        Frame(modifier = Modifier.padding(horizontal = Padding.LARGE, vertical = Padding.MEDIUM)) {
            Text(text = message, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TextInputDialog(
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textFieldShape: Shape = RoundedCornerShape(50),
    defaultValue: String = "",
    label: String? = null,
    subtitle: String? = null,
    vectorIcon: ImageVector? = null,
    title: String,
    onDismissRequest: (String?) -> Unit,
) {
    var text by remember {
        mutableStateOf(TextFieldValue(defaultValue, selection = TextRange(0, defaultValue.length)))
    }

    PrimeDialog(
        title = title,
        subtitle = subtitle,
        vectorIcon = vectorIcon,
        onDismissRequest = { onDismissRequest(null) },
        button1 = "Dismiss" to { onDismissRequest(null) },
        button2 = "Confirm" to { onDismissRequest(text.text) },
    ) {
        val focusRequester = remember {
            FocusRequester()
        }

        TextInputField(
            value = text,
            onValueChange = { text = it },
            textStyle = MaterialTheme.typography.h5.copy(
                fontWeight = FontWeight.Bold
            ),
            label = label,
            shape = textFieldShape,
            simple = false,
            leadingIcon = Icons.Outlined.Edit,
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .focusRequester(focusRequester = focusRequester)
                .padding(
                    vertical = Padding.EXTRA_LARGE,
                    horizontal = Padding.LARGE
                )
                .fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = { text = TextFieldValue("") },
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                }
            }
        )

        LaunchedEffect(key1 = "") {
            delay(500)
            focusRequester.requestFocus()
        }
    }
}


@JvmInline
value class Window(private val state: State<Boolean>) {
    fun show() {
        (state as MutableState).value = true
    }

    fun hide() {
        (state as MutableState).value = false
    }

    fun isVisible() = state.value

    fun toggle() = if (isVisible()) hide() else show()
}

@Stable
@Composable
fun memorize(content: @Composable Window.() -> Unit): Window {
    val switch = remember { Window(mutableStateOf(false)) }
    if (switch.isVisible()) content.invoke(switch)
    return switch
}

