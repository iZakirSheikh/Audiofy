/*
 * Copyright (c)  2025 Zakir Sheikh
 *
 * Created by sheik on 12 of Dec 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prime.media.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import coil.compose.AsyncImage
import com.prime.media.common.emit
import com.prime.media.common.fadingEdge2
import com.primex.material2.Text
import com.zs.core.db.Playlist.Track
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding

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
    modifier: Modifier = Modifier,
    artworkUri: String? = null
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(AppTheme.shapes.small) // Apply rounded corners
                then modifier
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
            style = AppTheme.typography.caption2,
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


private val NormalRecentItemArrangement = Arrangement.spacedBy(8.dp)

@Composable
context(_: RouteLibrary)
fun Recents(
    viewState: LibraryViewState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    // Collect recently played items from the Library state
    val recents by viewState.recent.collectAsState()
    // Display the list with loading, empty, and content states
    LazyRow(
        horizontalArrangement = NormalRecentItemArrangement,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier/*.fadingEdge2(20.dp, false)*/,
        contentPadding = contentPadding,
        content = {
            val values = emit(false, recents) ?: return@LazyRow item { Spacer(Modifier) }

            // Ensure first item is visible by adding a spacer at the front
            item(contentType = "library_list_spacer") {
                Spacer(modifier = Modifier)
            }

            items(values, key = Track::uri) {
                Recent(
                    it.title,             // Use the item's title
                    artworkUri = it.artwork,// Display artwork if available
                    modifier = Modifier.combinedClickable(
                        onClick = { viewState.onClickRecentFile(it.uri) }, // maybe play.
                        onLongClick = { viewState.onRequestRemoveRecentItem(it.uri) }
                    ).animateItem() // Animate item placement
                )
            }
        }
    )
}