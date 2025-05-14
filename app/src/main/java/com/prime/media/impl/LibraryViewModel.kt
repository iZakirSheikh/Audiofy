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

package com.prime.media.impl

import android.util.Log
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.prime.media.common.debounceAfterFirst
import com.prime.media.library.LibraryViewState
import com.zs.core.db.playlists.Playlist.Track
import com.zs.core.db.playlists.Playlists
import com.zs.core.store.MediaProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    val provider: MediaProvider, val playlists: Playlists
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
        TODO("Not yet implemented")
    }

    override fun onClickRecentAddedFile(id: Long) {
        TODO("Not yet implemented")
    }
}