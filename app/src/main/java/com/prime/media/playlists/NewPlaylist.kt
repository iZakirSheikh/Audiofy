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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text.input.rememberTextFieldState as TextFieldValue
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlaylistAddCircle
import androidx.compose.material.icons.outlined.Title
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.primex.material2.Button
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.Text
import com.zs.core.db.Playlist
import com.zs.core_ui.AppTheme
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP

private const val TAG = "NewPlaylist"


// Regex to validate folder names similar to Windows/Android rules, ensuring the name is not blank,
// does not start with an underscore, and does not contain special characters overall.
// ^(?!_): Ensures the name does not start with an underscore.
// [\\p{L}\\p{N}]: The first character must be a letter or a number.
// [^<>:\"/\\|?*\\s]*$: The subsequent characters must not be any special characters.
private val VALID_NAME_REGEX = Regex("^(?!_)[\\p{L}\\p{N}][\\p{L}\\p{N}^<>:\"/\\\\|?*\\s]*$")


private val hPadding = PaddingValues(horizontal = CP.normal, vertical = CP.medium)

/**
 * A composable that edits or generates a new playlist.
 * @param value optional value that is used to edit a playlist. If null, a new playlist is generated.
 * @param onRequest callback that is called when the request is made.
 */
@Composable
fun NewPlaylist(
    value: Playlist?,
    onConfirm: (value: Playlist?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = AppTheme.colors.background,
        shape = RoundedCornerShape(4),
        content = {
            //
            Column {
                // topBar
                TopAppBar(
                    title = {
                        Label(
                            stringResource(R.string.scr_new_playlists_title),
                            maxLines = 2,
                            style = AppTheme.typography.titleMedium
                        )
                    },
                    actions = {
                        IconButton(
                            Icons.Outlined.Close,
                            onClick = { onConfirm(null) },
                            modifier = Modifier
                        )
                    },
                    backgroundColor = AppTheme.colors.background(1.dp),
                    elevation = 0.dp,
                    navigationIcon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.padding(start = CP.medium)
                        )
                    }
                )
                // Title
                val title = TextFieldValue(value?.name ?: "")
                val isError by remember {
                    derivedStateOf { !VALID_NAME_REGEX.matches(title.text) }
                }
                OutlinedTextField(
                    title,
                    shape = AppTheme.shapes.compact,
                    label = { Label(stringResource(R.string.playlists_enter_playlist_name)) },
                    modifier = Modifier
                        .padding(hPadding)
                        .fillMaxWidth(),
                    lineLimits = SingleLine,
                    leadingIcon = {
                        Icon(Icons.Outlined.Title, contentDescription = null)
                    },
                    placeholder = {
                        Label(stringResource(R.string.playlists_text_field_placeholder))
                    },
                    isError = isError
                )

                // title
                val desc = TextFieldValue(value?.desc ?: "")
                OutlinedTextField(
                    desc,
                    label = { Label("Desc") },
                    shape = AppTheme.shapes.compact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(hPadding)
                        .weight(1f, false),
                    lineLimits = MultiLine(minHeightInLines = 5),
                    placeholder = { Text(stringResource(R.string.playlists_playlist_desc_placeholder)) }
                )
                Button(
                    stringResource(if (value == null) R.string.create else R.string.update),
                    onClick = {
                        onConfirm(
                            value?.clone(title.text.toString(), desc.text.toString())
                                ?: Playlist(title.text.toString(), desc.text.toString())
                        )
                    },
                    icon = rememberVectorPainter(Icons.Outlined.PlaylistAddCircle),
                    modifier = Modifier
                        .padding(hPadding)
                        .align(Alignment.End),
                    elevation = null,
                    enabled = !isError
                )
            }
        }
    )
}