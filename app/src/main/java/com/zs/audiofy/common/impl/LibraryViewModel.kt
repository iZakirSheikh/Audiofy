/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-05-2025.
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

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.zs.audiofy.common.impl

import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.library.LibraryViewState
import com.zs.compose.foundation.Rose
import com.zs.core.common.debounceAfterFirst
import com.zs.core.db.playlists.Playlist.Track
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.Remote
import com.zs.core.playback.toMediaFile
import com.zs.core.store.MediaProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    val provider: MediaProvider,
    val playlists: Playlists,
    val remote: Remote
) : KoinViewModel(), LibraryViewState {

    private val CAROUSAL_DELAY_MILLS = 10_000L // 10 seconds
    private val SHOW_CASE_MAX_ITEMS = 20


    override val recent: StateFlow<List<Track>?> =
        playlists.observer("_recent").stateIn(viewModelScope, SharingStarted.Lazily, null)

    override val carousel =
    // Observe changes to the media store URI. When a change occurs (e.g., new audio added),
        // the flow is re-evaluated.
        provider
            .observer(MediaProvider.EXTERNAL_AUDIO_URI)
            .debounceAfterFirst(500L)
            // Use flatMapLatest to cancel the previous flow and start a new one
            // whenever the external content URI changes.
            .flatMapLatest { _ ->
                val items = provider.fetchAudioFiles(
                    limit = SHOW_CASE_MAX_ITEMS,
                    order = MediaProvider.COLUMN_DATE_MODIFIED,
                    ascending = false
                )
                // If no items are found, maybe return a default value
                // Emit a default if no items
                if (items.isEmpty()) return@flatMapLatest flow { emit(-1L) }
                flow {
                    var current = 0
                    while (true) {
                        if (current >= items.size) current = 0 // Loop back to the beginning
                        emit(items[current].albumId) // Emit the ID of the current item
                        current++
                        // Wait for the delay before emitting the next item
                        delay(CAROUSAL_DELAY_MILLS)
                    }
                }
            }
            .catch {
                Log.d("TAG", "sds: $it")
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, -1L)


    override val newlyAdded = provider
        .observer(MediaProvider.EXTERNAL_AUDIO_URI)
        .debounceAfterFirst(500L)
        .map {
            provider.fetchAudioFiles(
                order = MediaProvider.COLUMN_DATE_MODIFIED,
                ascending = false,
                offset = 0,
                limit = SHOW_CASE_MAX_ITEMS
            )
        }
        .catch {
            Log.d("TAG", "ssd: $it")
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    override fun onClickRecentFile(uri: String) {
        // This function handles the click event for a recent file.
        // It determines if the clicked file is already in the current playlist.
        //
        // Case 1: The media item is already in the playlist.
        //   - In this case, the function simply seeks to the item's position in the playlist
        //     and starts playback.
        //
        // Case 2: The media item is not in the playlist.
        //   - The function first tries to find the item in the 'recent' list.
        //   - If found, the current playlist is replaced with a new playlist containing only this item,
        //     and playback starts.
        //   - If the item is not found in the 'recent' list (which is unexpected),
        //     an error message is displayed.
        runCatching {
            val index = remote.indexOf(Uri.parse(uri))
            val isAlreadyInPlaylist = index != Remote.INDEX_UNSET // Check if the item is in the current playlist.

            if (isAlreadyInPlaylist) {
                // If already in playlist, seek to it and play.
                remote.seekTo(index)
                remote.play(true)
                return@runCatching
            }

            // If not in the playlist, try to find it in the recent list.
            val recentItems = recent.firstOrNull() // Get the current list of recent items.
            val item = recentItems?.find { it.uri == uri } // Find the specific item by its URI.

            if (item == null) {
                // This case should ideally not happen if the UI is displaying valid recent items.
                showSnackbar("Oops! Unknown error", icon = Icons.Outlined.Error, accent = Color.Rose)
                return@runCatching
            }
            // If found in recent, create a new playlist with this item and play it.
            remote.setMediaFiles(listOf(item.toMediaFile())) // Replace current playlist.
            remote.play(true)
        }
    }

    override fun onClickRecentAddedFile(id: Long) {
        // This function handles the click event for a newly added file.
        // When a newly added file is clicked, this function ensures that only this specific file is played.
        runCatching {
            // Retrieve the list of newly added files.
            val newlyAddedItems = newlyAdded.firstOrNull()
            // Find the specific item by its ID.
            val item = newlyAddedItems?.find { it.id == id }
            if (item == null) {
                // This case should ideally not happen if the UI is displaying valid newly added items.
                showSnackbar("Oops! Unknown error", icon = Icons.Outlined.Error, accent = Color.Rose)
                return@runCatching
            }
            // Create a new playlist with this item and start playback.
            remote.setMediaFiles(listOf(item.toMediaFile()))
            remote.play(true)
        }
    }
}