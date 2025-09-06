/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.playlists

import android.text.format.DateUtils
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.emit
import com.zs.audiofy.common.compose.shine
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AlertDialog
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.ListItem
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.WindowSize
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.Text
import com.zs.core.db.playlists.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * Represents the playlists dialog.
 */
@Composable
fun Playlists(
    expanded: Boolean,
    values: Flow<List<Playlist>>,
    onSelect: (Playlist?) -> Unit
) {
    if (!expanded) return
    val (width, _) = LocalWindowSize.current
    val onDismissRequest = { onSelect(null) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        gravity = if (width <= WindowSize.Category.Medium) Gravity.BOTTOM else Gravity.CENTER,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        navigationIcon = {
            IconButton(Icons.Outlined.Close, onClick = onDismissRequest, contentDescription = null)
        },
        title = {
            Text(
                textResource(R.string.scr_playlists_dialog_empty_placeholder),
                maxLines = 2,
                fontWeight = FontWeight.Light
            )
        },
        content = {
            val data by values.collectAsState(null)
            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                val data = emit(true, data) ?: return@LazyColumn

                //
                items(data, contentType = { "playlist" }, key = Playlist::id) { value ->
                    ListItem(
                        heading = { Label(value.name) },
                        overline = {
                            Label(
                                pluralStringResource(
                                    R.plurals.files_d,
                                    value.count,
                                    value.count
                                )
                            )
                        },
                        subheading = {
                            Label(
                                DateUtils.getRelativeTimeSpanString(value.dateModified).toString()
                            )
                        },
                        leading = {
                            AsyncImage(
                                value.artwork,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .shadow(3.dp, shape = AppTheme.shapes.small)
                                    .size(110.dp, 64.dp)
                                    .border(AppTheme.colors.shine, AppTheme.shapes.small)
                                    .background(AppTheme.colors.background(1.dp))
                            )
                        },
                        modifier = Modifier
                            .clip(AppTheme.shapes.small)
                            .clickable {
                                onSelect(value)
                            }
                    )
                }
            }
        }
    )
}