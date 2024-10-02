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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zs.core_ui.ContentElevation
import com.zs.core_ui.ContentPadding
import com.prime.media.old.common.Artwork
import com.primex.material2.Text
import com.zs.core_ui.AppTheme

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
private fun RecentItem(
    label: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUri: String? = null
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(com.zs.core_ui.AppTheme.shapes.small) // Apply rounded corners
            .clickable(onClick = onClick) // Enable clicking
        //.then(modifier) // Apply additional modifiers
    ) {
        // Artwork section with border and shadow
        Artwork(
            data = artworkUri,
            modifier = Modifier
                .size(66.dp) // Adjust size if needed
                .border(2.dp, Color.White, RECENT_ICON_SHAPE) // Add white border
                .shadow(ContentElevation.low, RECENT_ICON_SHAPE) // Add subtle shadow
                .background(AppTheme.colors.background(1.dp))
        )

        // Label below the artwork with padding and styling
        Text(
            text = label,
            modifier = Modifier
                .padding(top = ContentPadding.medium)
                .width(80.dp),
            style = com.zs.core_ui.AppTheme.typography.caption,
            maxLines = 2, // Allow at most 2 lines for label
            textAlign = TextAlign.Center,
            minLines = 2
        )
    }
}


private val NormalRecentItemArrangement = Arrangement.spacedBy(8.dp)

/**
 * Composable function that displays a list of recently played items.
 *
 * @param state: The Library state containing recent items and click handling logic.
 * @param modifier: Optional modifier to apply to the list container.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentlyPlayedList(
    state: Library,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    // Collect recently played items from the Library state
    val recents by state.recent.collectAsState()
    // Display the list with loading, empty, and content states
    StatefulLazyList(
        items = recents,      // Provide the list of recent items
        key = { it.uri },    // Unique key for each item based on its URI
        modifier = modifier,  // Apply optional modifiers
        horizontalArrangement = NormalRecentItemArrangement,
        contentPadding = contentPadding,
        itemContent = {      // Define how to display each item
            RecentItem(
                it.title,             // Use the item's title
                onClick = { state.onClickRecentFile(it.uri) },  // Trigger click action
                modifier = Modifier.animateItem(null, fadeOutSpec = null),      // Animate item placement
                artworkUri = it.artwork,// Display artwork if available
            )
        }
    )
}

