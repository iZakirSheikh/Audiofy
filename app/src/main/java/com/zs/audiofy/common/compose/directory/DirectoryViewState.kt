/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
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

package com.zs.audiofy.common.compose.directory

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.vector.ImageVector
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Filter
import com.zs.audiofy.common.Mapped
import com.zs.audiofy.common.SelectionTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the common interface among directories
 *
 * This interface defines the common properties and behaviors expected from
 * a view state representing a directory of items.
 *
 * @param T The type of data items within the directory.
 * @property title The title of the directory, displayed in the top app bar.
 * @property focused The currently focused item within the directory. Defaults to `null`.
 * @property actions The list of available actions for the directory. These actions care displayed in the top app bar if [focused] is null or an action menu. Defaults to an empty list.
 * @property primaryAction The primary action for the directory, typically used for a Floating Action Button (FAB). Defaults to `null`.
 * @property favicon The icon to be used in place of the navigation icon in the top app bar. Defaults to `null`. If null, a back button might be shown.
 * @property orders The list of available actions for ordering the directory items.
 * @property data A [StateFlow] emitting the [Mapped] data for the directory.
 * @property query A [TextFieldState] representing the current search query for filtering.
 * @property filter A [Filter] object containing the current filtering parameters (e.g., sort order and ascending/descending).
 */
interface DirectoryViewState<T> {

    val title: CharSequence
    val primaryAction: Action? get() =  null
    val favicon: ImageVector? get() =  null
    val actions: List<Action> get() = emptyList()
    val orders: List<Action>
    val query: TextFieldState
    val filter: Filter

    var focused: T?
        get() = null
        set(value) = error("Not implemented yet!")

    val data: StateFlow<Mapped<T>?>

    // actions
    fun filter(ascending: Boolean = this.filter.first, order: Action = this.filter.second)
}

interface FilesViewState<T>: SelectionTracker {
    var info: MetaData
    val data: Mapped<T>?

    val filter: Filter
    val orders: List<Action>
    val query: TextFieldState

    val actions: List<Action>

    val favourites: Flow<Set<String>>

    fun play(item: T? = null)

    fun filter(order: Action = filter.second, ascending: Boolean = filter.first)

    fun shuffle()
}
