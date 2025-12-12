/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 20-10-2024.
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

package com.prime.media.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.primex.material2.Label
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.lottieAnimationPainter

private const val TAG = "lazy-ktx"

/**
 * A helper function that emits a placeholder item if the provided list is empty or null.
 * Otherwise, it returns the non-empty, non-null list.
 *
 * @return  item list, can be null if placeholder emitted otherwise no-null
 */
fun <T> LazyGridScope.emit(
    data: List<T>?,
): List<T>? {
    when {
        // null means loading
        data == null -> item(
            span = fullLineSpan,
            key = "key_loading_placeholder",
            contentType = "data_loading_placeholder",
            content = {
                Placeholder(
                    title = stringResource(R.string.loading),
                    iconResId = R.raw.lt_green_list_loading,
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
                    iconResId = R.raw.loading_hand,
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

inline fun <T> LazyListScope.emit(vertical: Boolean, data: List<T>?): List<T>? {
    when {
        // null means loading
        data == null && vertical -> item(contentType = "loading_vertical", key = "placeholder_loading_list") {
            Placeholder(
                title = stringResource(R.string.loading),
                iconResId = R.raw.lt_loading_bubbles,
                modifier = Modifier.fillMaxSize().animateItem()
            )
        }
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
        data.isEmpty() && vertical -> item(contentType = "empty_vertical", key = "placeholder_empty_list"){
            Placeholder(
                title = stringResource(R.string.empty),
                iconResId = R.raw.lt_empty_box,
                modifier = Modifier.fillMaxSize().animateItem()
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