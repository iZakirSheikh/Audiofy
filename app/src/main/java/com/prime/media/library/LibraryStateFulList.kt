/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 08-07-2024.
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

package com.prime.media.library

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.Placeholder


private val NormalRecentItemArrangement = Arrangement.spacedBy(8.dp)
/**
 * Composable function displaying a lazily loaded list of items with loading and empty states.
 *
 * @param items: The list of items to display, or null to show loading state.
 * @param modifier: Optional modifier to apply to the entire list container.
 * @param key: Optional key function for items, defaults to using item identity.
 * @param listState: The state of the lazy list for scrolling and performance.
 * @param itemContent: Composable function that defines the content for each item.
 */
@Composable
inline fun <T> StatefulLazyList(
    items: List<T>?,
    modifier: Modifier = Modifier,
    noinline key: ((item: T) -> Any)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    listState: LazyListState = rememberLazyListState(),
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    // Determine the current state of the list (loading, empty, or content)
    val state = when {
        items == null -> 0 // Loading state
        items.isEmpty() -> 1 // Empty state
        else -> 2 // Show list content
    }

    // Use Crossroad to smoothly transition between states based on state value
    Crossfade(
        targetState = state,
        modifier = modifier
    ) { value ->
        when (value) {
            // Loading state
            0 -> Placeholder(
                iconResId = R.raw.lt_loading_bubbles,
                title = "",
                message = stringResource(id = R.string.loading)
            )
            // Empty state
            1 -> Placeholder(
                iconResId = R.raw.lt_empty_box,
                title = "",
                message = stringResource(id = R.string.empty)
            )
            // Show list content
            else -> {
                LazyRow(
                    state = listState,
                    contentPadding = contentPadding,
                    horizontalArrangement = horizontalArrangement,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ensure first item is visible by adding a spacer at the front
                    item(contentType = "library_list_spacer") {
                        Spacer(modifier = Modifier.width(0.dp))
                    }

                    // Display actual items using "items" function
                    items(
                        items = items ?: emptyList(),
                        contentType = { "list_items" },
                        key = key,
                        itemContent = itemContent
                    )
                }
            }
        }
    }
}
