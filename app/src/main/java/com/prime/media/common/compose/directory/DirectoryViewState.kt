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

package com.prime.media.common.compose.directory

import androidx.compose.foundation.text.input.TextFieldState
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.Action
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the common interface among directories
 * @property data
 */
interface DirectoryViewState<T> {

    val title: CharSequence

    var focused: T?
        get() = null
        set(value) {
            TODO("Not yet implemented!")
        }
    val actions: List<Action> get() = emptyList()

    val orders: List<Action>
    val data: StateFlow<Mapped<T>?>

    // filter.
    val query: TextFieldState
    val filter: Filter

    // actions
    fun filter(ascending: Boolean = this.filter.first, order: Action = this.filter.second)
}