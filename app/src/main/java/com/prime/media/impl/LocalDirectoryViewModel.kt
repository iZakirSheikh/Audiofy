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

package com.prime.media.impl

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.prime.media.R
import com.prime.media.common.Action
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.SelectionTracker.Level
import com.prime.media.common.compose.FilterDefaults
import com.prime.media.common.compose.directory.DirectoryViewState
import com.prime.media.common.compose.directory.FilesViewState
import com.prime.media.common.debounceAfterFirst
import com.prime.media.common.raw
import com.zs.compose.theme.snackbar.SnackbarResult
import com.zs.core.playback.MediaFile
import com.zs.core.playback.Remote
import com.zs.core.store.MediaProvider
import com.zs.preferences.Key.Key2
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random.Default.nextInt

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

abstract class FilesViewModel<T>(val remote: Remote): KoinViewModel(), FilesViewState<T> {

    //common actions
    val ACTION_ADD_TO_PLAYLIST = Action(R.string.add_to_playlist, Icons.Outlined.PlaylistAdd)
    val ACTION_PLAY_NEXT = Action(R.string.play_next, Icons.Outlined.SkipNext)
    val ACTION_ADD_TO_QUEUE = Action(R.string.add_to_queue, Icons.AutoMirrored.Outlined.QueueMusic)
    val ACTION_SELECT_ALL = Action(R.string.select_all, Icons.Outlined.SelectAll)

    // Represents the
    override fun clear() = selected.clear()
    override val selected = mutableStateListOf<Long>()
    override val isInSelectionMode: Boolean by derivedStateOf(selected::isNotEmpty)
    override val allSelected: Boolean by derivedStateOf {
        // FixMe: For now this seems reasonable.
        //      However take the example of the case when size is same but ids are different
        selected.size == data?.values?.sumOf { it.size }
    }

    /**
     * Consumes the currently selected items and returns them as an array.
     *
     * This function creates a new array containing the selected items, clears the `selected` list, and returns the array.
     * @return An array containing the previously selected items.
     */
    fun consume(): LongArray {
        // Efficiently convert the list to an array.
        val data = selected.toLongArray()
        // Clear the selected items list.
        selected.clear()
        Log.d(TAG, "consume: ${data.size}")
        return data
    }

    abstract val T.id: Long
    abstract val T.asMediaFile: MediaFile

    override fun selectAll() {
        val data = data ?: return
        // Iterate through all items in the data and select them if they are not already selected.
        data.forEach {( _, items) ->
            items.forEach { item ->
                val id = item.id
                if (!selected.contains(id)) {
                    selected.add(id)
                }
            }
        }
    }

    override fun select(id: Long) {
        val contains = selected.contains(id)
        if (contains) selected.remove(id) else selected.add(id)
    }

    fun evaluateGroupSelectionLevel(key: String): Level {
        // Return NONE if data is not available.
        val data = data?.get(key) ?: return Level.NONE
        // Count selected
        val count = data.count { it.id in selected }
        return when (count) {
            data.size -> Level.FULL // All items in the group are selected.
            in 1..data.size -> Level.PARTIAL // Some items in the group are selected.
            else -> Level.NONE // No items in the group are selected.
        }
    }

    override fun isGroupSelected(key: String) =
        derivedStateOf { evaluateGroupSelectionLevel(key) }

    override fun select(key: String) {
        // Return if data is not available.
        val data = data ?: return
        // Get the current selection level of the group.
        val level = evaluateGroupSelectionLevel(key)
        // Get the IDs of all items in the group.
        val all = data[key]?.map { it.id } ?: emptyList()
        // Update the selected items based on the group selection level.
        when (level) {
            Level.NONE -> selected.addAll(all) // Select all items in the group.
            Level.PARTIAL -> selected.addAll(all.filterNot { it in selected }) // Select only unselected items.
            Level.FULL -> selected.removeAll(all.filter { it in selected }) // Deselect all selected items.
        }
    }

    override var data: Mapped<T>? by mutableStateOf(null)
    override val query: TextFieldState = TextFieldState()
    abstract val filterKey: Key2<String, Filter?>
    abstract override var filter: Filter
    override fun filter(order: Action, ascending: Boolean) {
        if (ascending == filter.first && order == filter.second) return
        // means only change in ascending happened
        // we don't support that, in order none.
        if (order == filter.second && order == FilterDefaults.ORDER_NONE && filter.first != ascending)
            return
        val newFilter = ascending to order
        preferences[filterKey] = newFilter
        filter = newFilter
    }

    private fun play(index: Int, shuffle: Boolean){
        viewModelScope.launch {
            val result = runCatching {
                // Determine the items to focus on for the action.
                val focused = when {
                    // If there are selected items, consume them.
                    // "consume()" clears the selection and returns the selected item IDs.
                    selected.isNotEmpty() -> consume().let {selected -> data?.values?.flatten()?.filter { it.id in selected } }
                    // Otherwise, if no items are selected, consider all items in the current view as focused.
                    else -> data?.values?.flatten()
                }
                if (focused.isNullOrEmpty())
                    error("Illegal state.")
                remote.setMediaFiles(focused.map {it.asMediaFile})
                remote.shuffle(shuffle)
                remote.play(true)
                showPlatformToast(message = "Playing\nPlaying tracks enjoy.")
            }
        }
    }

    override fun play(index: Int) = play(index, false)
    override fun shuffle() = play(-1, true)
}