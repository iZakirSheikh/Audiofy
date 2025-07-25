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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistAddCircle
import androidx.compose.material.icons.outlined.Title
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.zs.audiofy.R
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AlertDialog
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Button
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.WindowSize.Category
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.OutlinedTextField
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
    val (width, height) = LocalWindowSize.current
    if (!expanded) return
    val title = TextFieldValue(value?.name ?: "")
    val desc = TextFieldValue(value?.desc ?: "")
    val isError by remember {
        derivedStateOf { !PathUtils.VALID_NAME_REGEX.matches(title.text) }
    }
    AlertDialog(
        onDismissRequest = { onConfirm(null) },
        shape = AppTheme.shapes.medium,
        background = Background(AppTheme.colors.background),
        topBar = {
            TopAppBar(
                title = { Label(textResource(R.string.scr_new_playlists_title), maxLines = 2) },
                navigationIcon = {
                    Icon(
                        Icons.Outlined.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                },
                actions = {
                    IconButton(
                        icon = Icons.Outlined.Close,
                        contentDescription = null,
                        onClick = { onConfirm(null) }
                    )
                },
                elevation = 0.dp,
                background = Background(AppTheme.colors.background(2.dp))
            )
        },
        content = {
            //
            val view = LocalView.current
            val dialogWindowProvider = view.parent as? DialogWindowProvider
            if ((width < Category.Medium || height < Category.Medium) && dialogWindowProvider != null) {
                val gravity = if (width > height) Gravity.END else Gravity.BOTTOM
                SideEffect {
                    dialogWindowProvider.window.setGravity(gravity)
                }
            }

            // Title
            OutlinedTextField(
                title,
                shape = AppTheme.shapes.small,
                label = { Label(stringResource(R.string.scr_new_playlist_enter_name)) },
                modifier = Modifier
                    .fillMaxWidth(),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, false),
                lineLimits = MultiLine(minHeightInLines = 5),
                placeholder = { Label(stringResource(R.string.scr_new_playlist_desc_placeholder)) },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done
                ),
            )
        },
        bottomBar = {
            Button(
                stringResource(if (value == null) R.string.create else R.string.update),
                onClick = {
                    onConfirm(
                        value?.copy(title.text.toString(), desc.text.toString())
                            ?: Playlist(title.text.toString(), desc.text.toString())
                    )
                },
                icon = Icons.Outlined.PlaylistAddCircle,
                elevation = null,
                enabled = !isError
            )
        }
    )
}