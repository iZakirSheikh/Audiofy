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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.emit
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.text.Text
import com.zs.core.db.playlists.Playlist.Track

/**
 * The shape of the recent icon.
 */
private val RECENT_ICON_SHAPE = RoundedCornerShape(30)

/**
 * Composable function to create a clickable recent item with artwork and label.
 *
 * @param label: The CharSequence representing the item's label.
 * @param onClick: The action to perform when the item is clicked.
 * @param modifier: Optional modifier to apply to the item's layout.
 * @param artworkUri: Optional URI for the item's artwork image.
 */
@Composable
private fun Recent(
    label: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUri: String? = null
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(AppTheme.shapes.small) // Apply rounded corners
            .clickable(onClick = onClick) // Enable clicking
        //.then(modifier) // Apply additional modifiers
    ) {
        // Artwork section with border and shadow
        AsyncImage(
            model = artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(66.dp) // Adjust size if needed
                .border(2.dp, Color.White, RECENT_ICON_SHAPE) // Add white border
                .shadow(4.dp, RECENT_ICON_SHAPE) // Add subtle shadow
                .background(AppTheme.colors.background(1.dp))
        )

        // Label below the artwork with padding and styling
        Text(
            text = label,
            modifier = Modifier
                .padding(top = ContentPadding.small)
                .width(80.dp),
            style = AppTheme.typography.label3,
            maxLines = 2, // Allow at most 2 lines for label
            minLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Composable function that displays a list of recently played items.
 *
 * @param state: The Library state containing recent items and click handling logic.
 * @param modifier: Optional modifier to apply to the list container.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Recents(
    state: LibraryViewState,
    modifier: Modifier = Modifier
) {
    // Collect recently played items from the Library state
    val recents by state.recent.collectAsState()
    // Display the list with loading, empty, and content states
    LazyRow(
        horizontalArrangement = ContentPadding.SmallArrangement,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
        content = {
            val values = emit(false, recents) ?: return@LazyRow item { Spacer(Modifier) }

            // Ensure first item is visible by adding a spacer at the front
            item(contentType = "library_list_spacer") {
                Spacer(modifier = Modifier)
            }

            items(values, key = Track::uri) {
                Recent(
                    it.title,             // Use the item's title
                    onClick = { state.onClickRecentFile(it.uri) },  // Trigger click action
                    modifier = Modifier.animateItem(
                        null,
                        fadeOutSpec = null
                    ),      // Animate item placement
                    artworkUri = it.artwork,// Display artwork if available
                )
            }
        }
    )
}