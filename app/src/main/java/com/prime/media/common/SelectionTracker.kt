/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 24-10-2024.
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

import androidx.compose.runtime.State

/**
 * Provides a way to track selection state.
 * Each property is observable.
 *
 * @property focused The key of the currently focused item. Null if none is focused.
 * @property selected The list of keys representing the currently selected items.
 * @property isInSelectionMode Indicates whether selection mode is active.
 * @property allSelected True if all items are selected; otherwise, false.
 */
interface SelectionTracker {

    /**
     * Represents the selection level of a group.
     */
    enum class Level { NONE, PARTIAL, FULL }

    var focused: String?
    val selected: List<String>
    val isInSelectionMode: Boolean
    val allSelected: Boolean

    /**
     * Returns the selection level of the group represented by the key.
     *
     * @param key The key representing the group.
     * @return A `State` object representing the selection level of the group.
     * @throws UnsupportedOperationException If this function is not supported by the implementing class.
     */
    fun isGroupSelected(key: String): State<Level> {
        throw UnsupportedOperationException("This function is not supported by ${this::class.java.simpleName}")
    }

    /**
     * Toggles the selection of the group represented by the key.
     *
     * @param key The key representing the group.
     * @throws UnsupportedOperationException If this function is not supported by the implementing class.
     */
    fun check(key: String) {
        throw UnsupportedOperationException("This function is not supported by ${this::class.java.simpleName}")
    }

    /**
     * Toggles the selection of the item having the specified key.
     *
     * @param key The key representing the item.
     */
    fun select(key: String)

    /**
     * Clears the selection.
     */
    fun clear()

    /**
     * Selects all items.
     */
    fun selectAll()
}