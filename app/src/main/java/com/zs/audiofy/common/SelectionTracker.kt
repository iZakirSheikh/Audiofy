/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 19-07-2024.
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

package com.zs.audiofy.common

import androidx.compose.runtime.State

/**
 * Each property is observable.
 */
interface SelectionTracker {

    /** Represents the selection level of a group.*/
    enum class Level { NONE, PARTIAL, FULL }

    val selected: List<Long>

    /**
     * Indicates whether there is an active selection.
     */
    val isInSelectionMode: Boolean

    /**
     * @return true if all items are selected
     */
    val allSelected: Boolean

    /**
     * @return 0 if not; 1 if partially, 2 if fully selected.
     */

    fun isGroupSelected(key: String): State<Level> {
        throw UnsupportedOperationException("This function is not supported by ${this::class.java.simpleName}")
    }

    /**
     * Toggles the selection of group.
     */
    fun select(key: String) {
        throw UnsupportedOperationException("This function is not supported by ${this::class.java.simpleName}")
    }

    /**
     * Toggles the selection of item having [id]
     */
    fun select(id: Long)

    /**
     * Clears the selection.
     */
    fun clear()

    /**
     * Selects all items.
     */
    fun selectAll()
}