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

package com.zs.audiofy.common.impl

import android.net.Uri
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Filter
import com.zs.audiofy.common.Mapped
import com.zs.audiofy.common.compose.directory.DirectoryViewState
import com.zs.audiofy.common.raw
import com.zs.compose.theme.snackbar.SnackbarResult
import com.zs.core.common.debounceAfterFirst
import com.zs.core.store.MediaProvider
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

private const val TAG = "LocalDirectoryViewModel"

/**
 * Represents a view model for a local directory like folders, albums, artists, etc.
 */
abstract class LocalDirectoryViewModel<T>(
    val provider: MediaProvider
) : KoinViewModel(), DirectoryViewState<T> {
    abstract suspend fun fetch(filter: Filter, query: String? = null): Mapped<T>
    override val query: TextFieldState = TextFieldState()
    abstract override var filter: Filter


    abstract val uri: Uri

    override fun filter(ascending: Boolean, order: Action) {
        if (order == this.filter.second && this.filter.first == ascending) return
        this.filter = ascending to order
    }

    override val data: StateFlow<Mapped<T>?> = combine(
        snapshotFlow(query::raw).onStart { emit(null) }.drop(1),
        snapshotFlow(::filter),
        transform = { query, filter -> query to filter }
    )
        .debounceAfterFirst(300L)
        // transform it
        .flatMapLatest { (query, filter) ->
            provider.observer(uri).map {
                fetch(filter, query)
            }
        }
        // catch any exceptions.
        .catch {
            val report = report(it.message ?: getText(R.string.msg_unknown_error))
            if (report == SnackbarResult.ActionPerformed) analytics.record(it)
        }
        // make sure the flow is released after sometime.
        .stateIn(viewModelScope, WhileSubscribed(), null)
}