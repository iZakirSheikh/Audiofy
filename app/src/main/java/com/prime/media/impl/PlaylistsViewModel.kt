/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 19-10-2024.
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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.ui.graphics.Color
import com.prime.media.playlists.PlaylistsViewState
import com.primex.core.RedViolet
import com.zs.core.db.Playlist
import com.zs.core.db.Playlists
import com.zs.core_ui.toast.Toast
import kotlinx.coroutines.flow.Flow


// Regex to validate folder names similar to Windows/Android rules, ensuring the name is not blank,
// does not start with an underscore, and does not contain special characters overall.
// ^(?!_): Ensures the name does not start with an underscore.
// [\\p{L}\\p{N}]: The first character must be a letter or a number.
// [^<>:\"/\\|?*\\s]*$: The subsequent characters must not be any special characters.
private val VALID_NAME_REGEX = Regex("^(?!_)[\\p{L}\\p{N}][\\p{L}\\p{N}^<>:\"/\\\\|?*\\s]*$")

private const val TAG = "PlaylistsViewModel"

class PlaylistsViewModel(val playlists: Playlists) : KoinViewModel(), PlaylistsViewState {
    override val data: Flow<List<Playlist>> = playlists.observe()
    override fun create(value: Playlist) {
        tryLaunch {
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

    override fun delete(playlist: Playlist) {
        tryLaunch {
            val name = playlist.name
            // Display a confirmation toast to the user before deleting the playlist.
            // The toast includes the playlist name and a "Delete" action button.
            // If the user does not confirm (by clicking "Delete"), exit the coroutine.
            val result = showToast(
                message = "Are you sure you want to delete the playlist '$name'?",
                action = "Delete",
                icon = Icons.Outlined.FolderDelete,
                accent = Color.RedViolet,
                priority = Toast.PRIORITY_HIGH
            )
            if (result != Toast.ACTION_PERFORMED) return@tryLaunch
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
        tryLaunch {
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