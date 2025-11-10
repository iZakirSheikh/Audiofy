/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 20-10-2024.
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

package com.prime.media.playlists

import android.view.Gravity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.prime.media.R
import com.primex.core.textResource
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.Text
import com.zs.core.db.Playlist
import com.zs.core_ui.AlertDialog2
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Indication
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.TonalIconButton
import androidx.compose.foundation.text.input.rememberTextFieldState as TextFieldValue
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP

private const val TAG = "NewPlaylist"

// Regex to validate folder names similar to Windows/Android rules, ensuring the name is not blank,
// does not start with an underscore, and does not contain special characters overall.
// ^(?!_): Ensures the name does not start with an underscore.
// [\\p{L}\\p{N}]: The first character must be a letter or a number.
// [^<>:\"/\\|?*\\s]*$: The subsequent characters must not be any special characters.
private val VALID_NAME_REGEX = Regex("^(?!_)[\\p{L}\\p{N}][\\p{L}\\p{N}^<>:\"/\\\\|?*\\s]*$")

/**
 * A composable that edits or generates a new playlist.
 * @param value optional value that is used to edit a playlist. If null, a new playlist is generated.
 * @param onRequest callback that is called when the request is made.
 */
@Composable
fun NewPlaylistDialog(
    value: Playlist?,
    onConfirm: (value: Playlist?) -> Unit,
) {
    val (width, height) = LocalWindowSize.current
    val title = TextFieldValue(value?.name ?: "")
    val desc = TextFieldValue(value?.desc ?: "")
    val isError by remember {
        derivedStateOf { !VALID_NAME_REGEX.matches(title.text) }
    }
    val onDismissRequest = { onConfirm(null) }

    //
    val colors = AppTheme.colors
    AlertDialog2(
        onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                textResource(R.string.scr_new_playlists_title),
                style = AppTheme.typography.titleMedium,
                lineHeight = 20.sp,
                maxLines = 2
            )
        },
        navigationIcon = {
            IconButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                onClick = onDismissRequest
            )
        },
        gravity = if (width > height) Gravity.CENTER else Gravity.BOTTOM,
        actions = {
            val scale = Modifier.scale(0.80f).padding(end = CP.small)
            when {
                isError -> {}
                value != null -> TonalIconButton(
                    Icons.Outlined.Update,
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.update),
                    onClick = {
                        onConfirm(value.clone(title.text.toString(), desc.text.toString()))
                    },
                    modifier = scale
                )
                else -> TonalIconButton(
                    Icons.Outlined.Save,
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.create),
                    onClick = {
                        onConfirm(Playlist(title.text.toString(), desc.text.toString()))
                    },
                    modifier = scale
                )
            }
        },
        content = {
            // Title
            val padding = Modifier.padding(horizontal = CP.normal)
            OutlinedTextField(
                title,
                shape = AppTheme.shapes.compact,
                label = { Label(stringResource(R.string.playlists_enter_playlist_name)) },
                modifier = Modifier then padding.fillMaxWidth(),
                lineLimits = SingleLine,
                leadingIcon = {
                    Icon(Icons.Outlined.Title, contentDescription = null)
                },
                placeholder = {
                    Label(stringResource(R.string.playlists_text_field_placeholder))
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next
                ),
                isError = isError
            )

            // Desc
            OutlinedTextField(
                desc,
                label = { Label("Desc") },
                shape = AppTheme.shapes.compact,
                modifier = Modifier.fillMaxWidth() then padding.weight(1f, false),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = colors.onBackground.copy(ContentAlpha.Indication),
                    unfocusedIndicatorColor = Color.Transparent
                ),
                lineLimits = MultiLine(minHeightInLines = 5),
                placeholder = { Label(stringResource(R.string.playlists_playlist_desc_placeholder)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
            )
        }
    )
}