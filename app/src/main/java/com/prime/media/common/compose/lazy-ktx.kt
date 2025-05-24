/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 12-05-2025.
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

package com.prime.media.common.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.Mapped
import com.zs.compose.foundation.fullLineSpan
import com.zs.compose.theme.Icon
import com.zs.compose.theme.text.Label

fun <T> LazyListScope.emit(vertical: Boolean, data: List<T>?): List<T>? {
    when {
        // null means loading
        data == null -> item(contentType = "loading", key = "placeholder_loading_list") {
            Row(
                modifier = Modifier.sizeIn(minWidth = 320.dp, maxWidth = 360.dp, maxHeight = 56.dp).animateItem(),
                horizontalArrangement = Arrangement.spacedBy(ContentPadding.normal, alignment = Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    Icon(
                        painter = lottieAnimationPainter(R.raw.loading_hand),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                    )

                    Label(stringResource(R.string.loading))
                }
            )
        }
        data.isEmpty() -> item(contentType = "empty", key = "placeholder_empty_list") {
            Row(
                modifier = Modifier.sizeIn(minWidth = 320.dp, maxWidth = 360.dp, maxHeight = 56.dp).animateItem(),
                horizontalArrangement = Arrangement.spacedBy(ContentPadding.normal, alignment = Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    Icon(
                        painter = lottieAnimationPainter(R.raw.lt_empty_box),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )

                    Label(stringResource(R.string.empty))
                }
            )
        }
    }
    // return if non-empty else return null
    return data?.takeIf { it.isNotEmpty() }
}

/**
 * @see emit
 */
fun <T> LazyListScope.emit(
    data: Mapped<T>?
): Mapped<T>? {
    when {
        // null means loading
        data == null -> item(
            key = "key_loading_placeholder",
            contentType = "data_loading_placeholder",
            content = {
                Placeholder(
                    title = stringResource(R.string.loading),
                    iconResId = R.raw.lt_loading_bubbles,
                    modifier = Modifier.fillMaxSize().animateItem()
                )
            }
        )

        // empty means empty
        data.isEmpty() -> item(
            key = "key_empty_placeholder...",
            content = {
                Placeholder(
                    title = stringResource(R.string.empty),
                    iconResId = R.raw.lt_empty_box,
                    modifier = Modifier
                        .fillMaxSize()
                        .animateItem()
                )
            },
            contentType = "data_empty_placeholder"
        )
    }
    // return if non-empty else return null
    return data?.takeIf { it.isNotEmpty() }
}


/**
 * @see emit
 */
fun <T> LazyGridScope.emit(
    data: Mapped<T>?
): Mapped<T>? {
    when {
        // null means loading
        data == null -> item(
            span = fullLineSpan,
            key = "key_loading_placeholder",
            contentType = "data_loading_placeholder",
            content = {
                Placeholder(
                    title = stringResource(R.string.loading),
                    iconResId = R.raw.lt_loading_bubbles,
                    modifier = Modifier.fillMaxSize().animateItem()
                )
            }
        )

        // empty means empty
        data.isEmpty() -> item(
            span = fullLineSpan,
            key = "key_empty_placeholder...",
            content = {
                Placeholder(
                    title = stringResource(R.string.empty),
                    iconResId = R.raw.lt_empty_box,
                    modifier = Modifier
                        .fillMaxSize()
                        .animateItem()
                )
            },
            contentType = "data_empty_placeholder"
        )
    }
    // return if non-empty else return null
    return data?.takeIf { it.isNotEmpty() }
}

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
fun LazyListScope.stickyHeader(
    state: LazyListState,
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable LazyItemScope.() -> Unit
) {
    stickyHeader(
        key = key,
        contentType = contentType,
        content = {
            // TODO - Added parameter isPinned and fix the issue causing it to overlap.
            Layout (content = { content() }) { measurables, constraints ->
                val placeable = measurables[0].measure(constraints)
                val width = constraints.constrainWidth(placeable.width)
                val height = constraints.constrainHeight(placeable.height)
                layout(width, height) {
                    val posY = coordinates?.positionInParent()?.y?.toInt() ?: 0
                    val paddingTop = state.layoutInfo.beforeContentPadding
                    var top = (paddingTop - posY).coerceIn(0, paddingTop)
                    placeable.placeRelative(0, top)
                }
            }
        }
    )
}