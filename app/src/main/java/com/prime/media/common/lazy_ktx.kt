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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.prime.media.R

private const val TAG = "lazy-ktx"

/**
 * A helper function that emits a placeholder item if the provided list is empty or null.
 * Otherwise, it returns the non-empty, non-null list.
 *
 * @return  item list, can be null if placeholder emitted otherwise no-null
 */
inline fun <T> LazyGridScope.emit(
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

inline fun <T> LazyGridScope.emit(
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
