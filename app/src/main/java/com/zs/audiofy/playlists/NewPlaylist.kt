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

package com.zs.audiofy.playlists

import android.view.Gravity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.DialogProperties
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AlertDialog
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.TonalIconButton
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.OutlinedTextField
import com.zs.compose.theme.text.Text
import com.zs.compose.theme.text.TextFieldDefaults
import com.zs.core.common.PathUtils
import com.zs.core.db.playlists.Playlist
import androidx.compose.foundation.text.input.rememberTextFieldState as TextFieldValue

private const val TAG = "NewPlaylist"

/**
 * A composable that edits or generates a new playlist.
 * @param value optional value that is used to edit a playlist. If null, a new playlist is generated.
 * @param onRequest callback that is called when the request is made.
 */
@Composable
fun NewPlaylist(
    expanded: Boolean,
    value: Playlist?,
    onConfirm: (value: Playlist?) -> Unit,
) {
    if (!expanded) return
    val (width, height) = LocalWindowSize.current
    val title = TextFieldValue(value?.name ?: "")
    val desc = TextFieldValue(value?.desc ?: "")
    val isError by remember {
        derivedStateOf { !PathUtils.VALID_NAME_REGEX.matches(title.text) }
    }
    val onDismissRequest = { onConfirm(null) }

    //
    val colors = AppTheme.colors
    AlertDialog(
        onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                textResource(R.string.scr_new_playlists_title),
                fontWeight = FontWeight.Light,
                maxLines = 2
            )
        },
        navigationIcon = {
            IconButton(
                icon = Icons.Outlined.Close,
                contentDescription = null,
                onClick = onDismissRequest
            )
        },
        gravity = if (width > height) Gravity.CENTER else Gravity.BOTTOM,
        actions = {
            val scale = Modifier.scale(0.80f).padding(end = ContentPadding.small)
            when {
                isError -> {}
                value != null -> TonalIconButton(
                    Icons.Outlined.Update,
                    contentDescription = stringResource(R.string.update),
                    onClick = {
                        onConfirm(value.copy(title.text.toString(), desc.text.toString()))
                    },
                    modifier = scale
                )
                else -> TonalIconButton(
                    Icons.Outlined.Save,
                    contentDescription = stringResource(R.string.create),
                    onClick = {
                        onConfirm(Playlist(title.text.toString(), desc.text.toString()))
                    },
                    modifier = scale
                )
            }
        },
        content = {
            // Title
            val padding = Modifier.padding(horizontal = ContentPadding.normal)
            OutlinedTextField(
                title,
                shape = AppTheme.shapes.small,
                label = { Label(stringResource(R.string.scr_new_playlist_enter_name)) },
                modifier = Modifier then padding.fillMaxWidth(),
                lineLimits = SingleLine,
                leadingIcon = {
                    Icon(Icons.Outlined.Title, contentDescription = null)
                },
                placeholder = {
                    Label(stringResource(R.string.scr_new_playlist_text_field_placeholder))
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
                shape = AppTheme.shapes.small,
                modifier = Modifier.fillMaxWidth() then padding.weight(1f, false),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = colors.onBackground.copy(ContentAlpha.indication),
                    unfocusedIndicatorColor = Color.Transparent
                ),
                lineLimits = MultiLine(minHeightInLines = 5),
                placeholder = { Label(stringResource(R.string.scr_new_playlist_desc_placeholder)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
            )
        },
        shape = AppTheme.shapes.large,
    )
}
