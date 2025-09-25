/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 25-09-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.audiofy.library

import android.view.Gravity
import android.webkit.URLUtil
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Link
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.zs.audiofy.R
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AlertDialog
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Button
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.TonalIconButton
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.OutlinedTextField
import com.zs.compose.theme.text.Text
import com.zs.compose.theme.text.TextFieldDefaults
import androidx.compose.foundation.text.input.rememberTextFieldState as TextFieldValue
import com.zs.audiofy.common.compose.ContentPadding as CP

private val SchemeRegex = Regex("^(\\w+):")
private val SchemeHighlight = OutputTransformation {
    // Find the first match of the SchemeRegex in the current text of the TextField.
    // If no match is found (e.g., the text is empty or doesn't start with a scheme),
    // return from the transformation without applying any style.
    val match = SchemeRegex.find(toString()) ?: return@OutputTransformation

    // Get the range (start and end index) of the matched scheme.
    val schemeRange = match.range

    // Apply a SpanStyle to the matched scheme range.
    // The style changes the color of the scheme text to Green.
    addStyle(
        SpanStyle(color = Color.Green),
        start = schemeRange.first, // Start index of the scheme.
        end = schemeRange.last + 1   // End index of the scheme (inclusive, hence +1).
    )
}

/**
 * Dialog for entering a new stream link.
 * @param expanded True if the dialog should be shown, false otherwise.
 * @param onNewLink Callback invoked with the entered URL or null if dismissed.
 */
@Composable
fun NewMediaLink(
    expanded: Boolean,
    onNewLink: (link: String?) -> Unit
) {
    val onDismissRequest = { onNewLink(null) }
    val (width, height) = LocalWindowSize.current
    AlertDialog(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        navigationIcon = {
            Icon(
                Icons.Outlined.Link,
                contentDescription = null,
                modifier = Modifier.minimumInteractiveComponentSize(),
            )
        },
        title = {
            Text(
                textResource(R.string.scr_network_stream_title), fontWeight = FontWeight.Light,
                maxLines = 2
            )
        },
        actions = {
            TonalIconButton(
                Icons.Outlined.Close,
                contentDescription = null,
                onClick = onDismissRequest,
                color = LocalContentColor.current,
                modifier = Modifier
                    .scale(0.9f)
                    .padding(end = CP.small)
            )
        },
        properties = DialogProperties(usePlatformDefaultWidth = height < width),
        gravity = if (width > height) Gravity.CENTER else Gravity.BOTTOM,
        content = {
            val link = TextFieldValue("")
            val isError by remember {
                derivedStateOf {
                    !URLUtil.isValidUrl(link.text.toString())
                }
            }
            val clipboard = LocalClipboardManager.current
            Header(
                "Let’s get streaming—enter your URL.",
                style = AppTheme.typography.label3,
                color = AppTheme.colors.accent,
                modifier = Modifier.padding(horizontal = CP.large)
            )
            // Link
            OutlinedTextField(
                link,
                shape = AppTheme.shapes.small,
                placeholder = { Label("http://example.com/stream.mp3") },
                modifier = Modifier
                    .padding(horizontal = CP.normal, vertical = CP.small)
                    .fillMaxWidth(),
                lineLimits = SingleLine,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    backgroundColor = AppTheme.colors.background(6.dp)
                ),
                outputTransformation = SchemeHighlight,
                isError = isError,
                trailingIcon = {
                    when {
                        link.text.isNotEmpty() -> IconButton(
                            Icons.Outlined.Close,
                            contentDescription = null,
                            onClick = {
                                link.clearText()
                            }
                        )

                        clipboard.hasText() -> IconButton(
                            Icons.Outlined.Assignment,
                            contentDescription = null,
                            onClick = {
                                link.clearText()
                                link.edit {
                                    insert(0, clipboard.getText()?.toString() ?: "")
                                }
                            }
                        )

                        else -> Unit
                    }
                }
            )
            // Button
            Button(
                stringResource(R.string.play),
                onClick = { onNewLink(link.text.toString()) },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = CP.medium),
                enabled = !isError
            )
        }
    )
}