/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 14-05-2025.
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

import android.text.format.DateUtils
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FeaturedPlayList
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Filter
import com.zs.audiofy.common.Mapped
import com.zs.audiofy.common.compose.FilterDefaults
import com.zs.audiofy.common.raw
import com.zs.audiofy.playlists.PlaylistsViewState
import com.zs.compose.foundation.RedViolet
import com.zs.compose.foundation.castTo
import com.zs.compose.theme.snackbar.SnackbarDuration
import com.zs.compose.theme.snackbar.SnackbarResult
import com.zs.core.common.debounceAfterFirst
import com.zs.core.db.playlists.Playlist
import com.zs.core.db.playlists.Playlists
import com.zs.preferences.stringPreferenceKey
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.util.Locale


// Regex to validate folder names similar to Windows/Android rules, ensuring the name is not blank,
// does not start with an underscore, and does not contain special characters overall.
// ^(?!_): Ensures the name does not start with an underscore.
// [\\p{L}\\p{N}]: The first character must be a letter or a number.
// [^<>:\"/\\|?*\\s]*$: The subsequent characters must not be any special characters.
private val VALID_NAME_REGEX = Regex("^(?!_)[\\p{L}\\p{N}][\\p{L}\\p{N}^<>:\"/\\\\|?*\\s]*$")

private val ORDER_NONE = FilterDefaults.ORDER_NONE
private val ORDER_NAME = FilterDefaults.ORDER_BY_TITLE
private val ORDER_BY_MODIFIED = FilterDefaults.ORDER_BY_DATE_MODIFIED

private val ACTION_DELETE = Action(R.string.delete, Icons.Outlined.FolderDelete)
private val ACTION_EDIT = Action(R.string.edit, Icons.Outlined.EditNote)
private val ACTION_CREATE = Action(R.string.create, Icons.Outlined.PlaylistAdd)

// FIXME: Might cause crash.
private val Playlist.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

class PlaylistsViewModel(val playlists: Playlists) : KoinViewModel(), PlaylistsViewState {


    override var focused: Playlist? by mutableStateOf(null)
    override val orders: List<Action> = listOf(ORDER_NONE, ORDER_NAME, ORDER_BY_MODIFIED)
    override val title: CharSequence = getText(R.string.playlists)
    override val query: TextFieldState = TextFieldState()
    override val favicon: ImageVector? = Icons.Outlined.FeaturedPlayList
    override var showEditDialog: Boolean by mutableStateOf(false)

    override val primaryAction: Action? = ACTION_CREATE
    override val actions: List<Action> by derivedStateOf {
        if (focused == null) return@derivedStateOf emptyList()
        buildList {
            this += ACTION_DELETE; this += ACTION_EDIT
        }
    }

    val filterKey = stringPreferenceKey(
        "playlists_filter_key",
        null,
        FilterDefaults.FilterSaver { id ->
            when (id) {
                ORDER_NAME.id -> ORDER_NAME
                ORDER_BY_MODIFIED.id -> ORDER_BY_MODIFIED
                else -> ORDER_NONE
            }
        }
    )

    override var filter: Filter by mutableStateOf(preferences[filterKey] ?: (true to ORDER_NONE))

    override fun filter(ascending: Boolean, order: Action) {
        if (ascending == filter.first && order == filter.second) return
        val newFilter = ascending to order
        preferences[filterKey] = newFilter
        filter = newFilter
    }

    override val data: StateFlow<Mapped<Playlist>?> = combine(
        snapshotFlow(query::raw).onStart { emit(null) }.drop(1),
        snapshotFlow(::filter),
        transform = { query, filter -> query to filter }
    )
        .debounceAfterFirst(300L)
        // transform it
        .flatMapLatest { (query, filter) ->
            val (ascending, order) = filter
            playlists.observe(query).map { playlists ->
                val result = when (order) {
                    ORDER_NONE -> playlists.groupBy { "" }
                    ORDER_NAME -> playlists.sortedBy { it.firstTitleChar }
                        .let { if (ascending) it else it.reversed() }.groupBy { it.firstTitleChar }

                    ORDER_BY_MODIFIED -> playlists.sortedBy { it.dateModified }
                        .let { if (ascending) it else it.reversed() }
                        .groupBy { DateUtils.getRelativeTimeSpanString(it.dateModified).toString() }

                    else -> error("Oops!! invalid order passed $order")
                }
                // This should be safe
                castTo(result) as Mapped<Playlist>
            }
        }
        // catch any exceptions.
        .catch {
            val report = report(it.message ?: getText(R.string.msg_unknown_error))
            if (report == SnackbarResult.ActionPerformed) analytics.record(it)
        }
        // make sure the flow is released after sometime.
        .stateIn(viewModelScope, WhileSubscribed(), null)

    override fun create(value: Playlist) {
        this@PlaylistsViewModel.runCatching {
            // Validate the playlist name using the predefined regex.
            // If the name is invalid, throw an exception with a descriptive message.
            if (!VALID_NAME_REGEX.matches(value.name))
                error("The provided name is invalid.")

            // Check if a playlist with the same name already exists.
            // If a playlist with the same name exists, throw an exception.
            if (playlists.exists(value.name))
                error("A playlist named ${value.name} already exists.")
            // Add the new playlist to the database.
            // and display a success message to the user.
            playlists.insert(value)
            showPlatformToast("Playlist ${value.name} created successfully!")
        }
    }

    override fun onAction(action: Action) {
        when(action.id){
            ACTION_CREATE.id, ACTION_EDIT.id -> { showEditDialog = true }
            ACTION_DELETE.id -> { focused?.let(::delete) }
        }
    }

    override fun delete(playlist: Playlist) {
        runCatching {
            val name = playlist.name
            // Display a confirmation toast to the user before deleting the playlist.
            // The toast includes the playlist name and a "Delete" action button.
            // If the user does not confirm (by clicking "Delete"), exit the coroutine.
            val result = showSnackbar(
                message = "Are you sure you want to delete the playlist '$name'?",
                action = "Delete",
                icon = Icons.Outlined.FolderDelete,
                accent = Color.RedViolet,
                duration = SnackbarDuration.Long
            )
            if (result != SnackbarResult.ActionPerformed) return@runCatching
            // Attempt to delete the playlist from the data source (e.g., database).
            // Check if the deletion was successful (assuming delete() returns 1 for success).
            // If the deletion fails, throw an exception with a descriptive message.
            val success = playlists.delete(playlist.id) == 1
            if (!success)
                error("Some unknown error occurred while deleting the playlist '$name'.")
            // Display a success message to the user after successful deletion.
            showPlatformToast(message = "Playlist '$name' has been deleted successfully.")
        }
    }

    override fun update(playlist: Playlist) {
        this@PlaylistsViewModel.runCatching {
            val name = playlist.name
            // Validate the playlist name using the predefined regex.
            // If the name is invalid, throw an exception with a descriptive message.
            if (!VALID_NAME_REGEX.matches(name))
                error("The provided name '$name' is invalid.")

            // Check for existing playlist with the same name (excluding the current playlist being updated).
            // Use playlists[name] to efficiently check if a playlist with the given name exists.
            // If a playlist with the same name exists (and it's not the current playlist), throw an exception.
            val existingPlaylistId = playlists[name]?.id
            if (existingPlaylistId != null && existingPlaylistId != playlist.id)
                error("A playlist named '$name' already exists.")

            // Update the playlist in the data source (e.g., database).
            // If no rows are updated, it indicates an error, so throw an exception.
            val rowsUpdated = playlists.update(playlist)
            if (rowsUpdated == 0)
                error("Some unknown error occurred while updating the playlist '$name'.")

            // Display a success message to the user using a platform-specific toast.
            showPlatformToast(message = "Playlist '$name' has been updated successfully.")
        }
    }
}