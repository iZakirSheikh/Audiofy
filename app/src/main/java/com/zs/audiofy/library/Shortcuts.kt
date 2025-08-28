/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 11-05-2025.
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


import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.zs.audiofy.R
import com.zs.audiofy.audios.directory.RouteAlbums
import com.zs.audiofy.audios.directory.RouteArtists
import com.zs.audiofy.audios.directory.RouteGenres
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.shapes.FolderShape
import com.zs.audiofy.folders.RouteFolders
import com.zs.audiofy.playlists.members.RouteMembers
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Icon
import com.zs.compose.theme.ripple
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Label
import com.zs.core.playback.Remote

/**
 * Composable function to create a clickable shortcut with an icon and label.
 *
 * @param icon: The ImageVector representing the shortcut's icon.
 * @param label: The CharSequence representing the shortcut's label.
 * @param onAction: The action to perform when the shortcut is clicked.
 * @param modifier: Optional modifier to apply to the shortcut's layout.
 */
@Composable
private fun Shortcut(
    icon: ImageVector,
    label: CharSequence,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Base container for the shortcut with styling and click handling
    val colors = AppTheme.colors
    val accent = colors.onBackground
    Box(
        modifier = modifier
            .clip(FolderShape) // Shape the shortcut like a folder
            // .background(colors.primary.copy(0.035f), FolderShape)
            .border(1.dp, accent.copy(0.40f), FolderShape) // Light border
            //  .background(colors.backgroundColorAtElevation(0.4.dp), FolderShape)
            .clickable(
                null,
                ripple(true, color = AppTheme.colors.accent), // Ripple effect on click
                role = Role.Button, // Semantically indicate a button
                onClick = onAction // Trigger the action on click
            )
            .padding(horizontal = 8.dp, vertical = 8.dp) // Add internal padding
            .size(68.dp, 56.dp) // Set size (adjust factor if needed)
        // then modifier // Apply additional modifiers
    ) {
        // Icon at the top
        Icon(
            imageVector = icon,
            contentDescription = null, // Ensure a content description is provided elsewhere
            tint = accent,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Label at the bottom
        Label(
            text = label,
            style = AppTheme.typography.body3,
            color = accent,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Shortcuts(
    modifier: Modifier = Modifier,
) {
    // FlowRow to arrange shortcuts horizontally with spacing
    FlowRow(
        modifier = modifier/*.scaledLayout(1.3f)*/,
        horizontalArrangement = ContentPadding.SmallArrangement,
        verticalArrangement = ContentPadding.xSmallArrangement,
        content = {
            val navigator = LocalNavController.current

            // Shortcut for Genres navigation
            Shortcut(
                onAction = { navigator.navigate(RouteAlbums()) },
                icon = Icons.Outlined.Album,
                label = textResource(id = R.string.albums),
            )

            // Shortcut for Genres navigation
            Shortcut(
                onAction = { navigator.navigate(RouteGenres()) },
                icon = Icons.Outlined.Grain,
                label = textResource(id = R.string.genres),
            )

            // Shortcut for Artists navigation
            Shortcut(
                onAction = { navigator.navigate(RouteArtists()) },
                icon = Icons.Outlined.Person,
                label = textResource(id = R.string.artists),
            )

            // Favourites
            Shortcut(
                onAction = { navigator.navigate(RouteMembers(Remote.PLAYLIST_FAVOURITE)) },
                icon = Icons.Outlined.FolderSpecial,
                label = textResource(id = R.string.liked),
            )

            //
            Header(
                textResource(R.string.folders),
                style = AppTheme.typography.label3,
                color = AppTheme.colors.accent,
                modifier = Modifier.padding(top = ContentPadding.small)
            )
            // Audio Folders
            Shortcut(
                onAction = { navigator.navigate(RouteFolders(true)) },
                icon = Icons.Outlined.LibraryMusic,
                label = "Audio",
            )

            Shortcut(
                onAction = { navigator.navigate(RouteFolders(false)) },
                icon = Icons.Outlined.VideoLibrary,
                label = "Video",
            )
        }
    )
}