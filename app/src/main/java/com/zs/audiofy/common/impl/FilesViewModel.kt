/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 20-07-2025.
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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Filter
import com.zs.audiofy.common.Mapped
import com.zs.audiofy.common.PLAY_NEXT
import com.zs.audiofy.common.SelectionTracker.Level
import com.zs.audiofy.common.compose.directory.FilesViewState
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.MediaFile
import com.zs.core.playback.Remote
import com.zs.preferences.Key.Key2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


abstract class FilesViewModel<T>(val remote: Remote, val playlists: Playlists): KoinViewModel(), FilesViewState<T> {

    //common actions
    val ACTION_ADD_TO_PLAYLIST = Action(R.string.add_to_playlist, Icons.Outlined.PlaylistAdd)
    val ACTION_PLAY_NEXT = Action.PLAY_NEXT
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

    override val favourites: Flow<Set<String>> =
        playlists.observeKeysOf(Remote.PLAYLIST_FAVOURITE).map(List<String>::toHashSet)

    /**
     * Consumes the selected items and returns a list of focused items.
     *
     * If no items are selected, all items in the current view are considered focused.
     * Otherwise, the selected items are consumed (cleared from the selection list) and
     * the corresponding items from the data are returned as focused.
     *
     * @return A list of focused items. Returns an empty list if data is null.
     */
    fun consume(): List<T> {
        // Determine the items to focus on for the action.
        val focused = when {
            // Otherwise, if no items are selected, consider all items in the current view as focused.
            selected.isEmpty() -> data?.values?.flatten()
            else -> {
                // Efficiently convert the list to an array.
                val consumed = selected.toLongArray()
                // Clear the selected items list.
                selected.clear()
                consumed.let {selected -> data?.values?.flatten()?.filter { it.key in selected } }
            }
        }
        return  focused ?: emptyList()
    }

    abstract val T.key: Long

    override fun selectAll() {
        val data = data ?: return
        // Iterate through all items in the data and select them if they are not already selected.
        data.forEach {( _, items) ->
            items.forEach { item ->
                val id = item.key
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
        val count = data.count { it.key in selected }
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
        val all = data[key]?.map { it.key } ?: emptyList()
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
        val newFilter = ascending to order
        preferences[filterKey] = newFilter
        filter = newFilter
    }

    suspend fun play(items: List<MediaFile>, index: Int = -1, shuffle: Boolean = false){
        // set new media files
        remote.setMediaFiles(items)
        remote.shuffle(shuffle)
        remote.play(true)
        // when new playlist is loaded ensure; repeat mode is set to all because idiots don't seem
        // to notice.
        remote.setRepeatMode(Remote.REPEAT_MODE_OFF)
        if (index != -1)
            remote.seekTo(index)
        showPlatformToast(message = R.string.playing)
    }

    fun addToQueue(items: List<MediaFile>){
        runCatching {
            // Currently, if items are already present in the queue, we simply skip adding them again
            // and display a message indicating this.
            // A potential enhancement could be to remove these existing items from their current
            // positions in the queue and then re-add them to the end. This would effectively move them.
            val result = remote.add(items)
            val message = when {
                result == 0 -> "No items were added — they are already in the queue."
                result < items.size -> "Added $result of ${items.size} items — some were skipped as they’re already in the queue."
                else -> "Queue updated."
            }

            // Then display it however you like:
            showSnackbar(message) // or showPlatformToast(message), depending on the case
        }
    }

    fun playNext(items: List<MediaFile>){
        runCatching {
            val result = remote.add(items, remote.getNextMediaItemIndex())
            val message = when {
                result == 0 -> "No items were added — they are already in the queue."
                result < items.size -> "Added $result of ${items.size} items — some were skipped as they’re already in the queue."
                else -> "Queue updated."
            }

            // Then display it however you like:
            showSnackbar(message) // or showPlatformToast(message), depending on the case
        }
    }
}