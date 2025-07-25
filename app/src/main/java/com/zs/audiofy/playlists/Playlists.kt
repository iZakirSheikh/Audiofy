/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.directory.Directory
import com.zs.audiofy.common.compose.scale
import com.zs.audiofy.playlists.members.RouteMembers
import androidx.compose.foundation.combinedClickable as clickable

private val MIN_CELL_WIDTH = 120.dp

@Composable
fun Playlists(viewState: PlaylistsViewState) {

    val focused = viewState.focused
    NewPlaylist(
        viewState.showEditDialog, focused, onConfirm = { newPlaylist ->
            when {
                newPlaylist == null -> {}
                focused != null -> viewState.update(newPlaylist)
                else -> viewState.create(newPlaylist)
            }
            viewState.focused = null
            viewState.showEditDialog = false
        })

    val navController = LocalNavController.current
    Directory(
        viewState, minSize = MIN_CELL_WIDTH, onActionClick = viewState::onAction
    ) { playlist ->
        // TODO Explore the use of derived state to calculate the 'focused' value
        //  instead of directly comparing 'highlighted' and 'playlist'. Derived state can improve
        //  performance and ensure consistency.
        // Determine if the current playlist is focused (selected)
        val focused = focused == playlist

        PlaylistItem(
            value = playlist,
            focused = focused,
            modifier = Modifier
                .animateItem()
                .clickable(
                    null, indication = scale(),
                    // On long click, toggle the focused state of the playlist item
                    onLongClick = {
                        viewState.focused = if (!focused) playlist else null
                    }, onClick = {
                        when {
                            focused -> viewState.focused = null // If already focused, unfocused it
                            viewState.focused != null -> viewState.focused =
                                playlist // make it move focus
                            else -> navController.navigate(RouteMembers(playlist.name)) // Navigate to the playlist details
                        }
                    }))
    }
}