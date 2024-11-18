/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 18-11-2024.
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

package com.zs.core_ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

private const val TAG = "LazyList"

/**
 * A sticky header implementation that respects the top padding of the content.
 * This should be removed when an official solution is provided.
 * Currently, the only issue is that the sticky layout and the next item overlap before moving,
 * while the sticky header should start moving when the next item is about to become sticky.
 *
 * @param state The state of the LazyGrid.
 * @param key The key for the sticky header item.
 * @param contentType The type of content for the sticky header.
 * @param content The composable content for the sticky header.
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyGridScope.stickyHeader(
    state: LazyGridState,
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyGridItemScope.() -> Unit
) {
    stickyHeader(
        key = key,
        contentType = contentType,
        content = {
            Layout(content = { content() }) { measurables, constraints ->
                val placeable = measurables[0].measure(constraints)
                val width = constraints.constrainWidth(placeable.width)
                val height = constraints.constrainHeight(placeable.height)
                layout(width, height) {
                    val posY = coordinates?.positionInParent()?.y?.toInt() ?: 0
                    val paddingTop = state.layoutInfo.beforeContentPadding
                    var top = (paddingTop - posY).coerceIn(0, paddingTop)
                    placeable.placeRelativeWithLayer(0, top)
                }
            }
        }
    )
}
