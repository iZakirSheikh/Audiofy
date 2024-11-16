/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 07-07-2024.
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

package com.prime.media.old.library

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.local.videos.RouteVideos
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.directory.playlists.Members
import com.prime.media.old.directory.store.Artists
import com.prime.media.old.directory.store.Audios
import com.prime.media.old.directory.store.Genres
import com.primex.core.textResource
import com.primex.material2.Label
import com.zs.core.playback.Playback
import com.zs.core_ui.AppTheme
import com.zs.core_ui.shape.FolderShape

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
    val colors =  AppTheme.colors
    val accent = colors.onBackground
    Box(
        modifier = modifier
            .clip(FolderShape) // Shape the shortcut like a folder
            // .background(colors.primary.copy(0.035f), FolderShape)
            .border(1.dp, accent.copy(0.4f), FolderShape) // Light border
          //  .background(colors.backgroundColorAtElevation(0.4.dp), FolderShape)
            .clickable(
                null,
                ripple(true, color = AppTheme.colors.accent), // Ripple effect on click
                role = Role.Button, // Semantically indicate a button
                onClick = onAction // Trigger the action on click
            )
            .padding(horizontal = 8.dp, vertical = 8.dp) // Add internal padding
            .size(70.dp, 58.dp) // Set size (adjust factor if needed)
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
            style = AppTheme.typography.caption,
            color = accent,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

private val NormalRecentItemArrangement = Arrangement.spacedBy(8.dp)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Shortcuts(
    modifier: Modifier = Modifier,
) {
    // FlowRow to arrange shortcuts horizontally with spacing
    FlowRow(
        modifier = modifier/*.scaledLayout(1.3f)*/,
        horizontalArrangement = NormalRecentItemArrangement,
        verticalArrangement = NormalRecentItemArrangement,
        content = {
            val navigator = LocalNavController.current

            // Removed commented-out "Folders" shortcut for now
            // Add it back when implemented or provide an explanation

            // Shortcut for Genres navigation
            Shortcut(
                onAction = { navigator.navigate(Genres.direction()) },
                icon = Icons.Outlined.Grain,
                label = textResource(id = R.string.genres),
            )

            // Shortcut for Audios navigation
            Shortcut(
                onAction = { navigator.navigate(Audios.direction(Audios.GET_EVERY)) },
                icon = Icons.Outlined.GraphicEq,
                label = textResource(id = R.string.audios),
            )

            // Shortcut for Artists navigation
            Shortcut(
                onAction = { navigator.navigate(Artists.direction()) },
                icon = Icons.Outlined.Person,
                label = textResource(id = R.string.artists),
            )

            // Shortcut for Video Library
            Shortcut(
                onAction = { navigator.navigate(RouteVideos()) },
                icon = Icons.Outlined.VideoLibrary,
                label = textResource(id = R.string.videos),
            )

            // Shortcut for Favourite playlist navigation
            Shortcut(
                onAction = { navigator.navigate(Members.direction(Playback.PLAYLIST_FAVOURITE)) },
                icon = Icons.Outlined.FavoriteBorder,
                label = textResource(id = R.string.favourite),
            )
        }
    )
}
