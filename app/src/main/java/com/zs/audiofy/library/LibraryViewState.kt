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

package com.zs.audiofy.library

import com.zs.audiofy.common.Route
import com.zs.core.db.playlists.Playlist.Track
import com.zs.core.store.models.Audio
import kotlinx.coroutines.flow.StateFlow

object RouteLibrary: Route

/**
 * Represents the state of the library screen.
 */
interface LibraryViewState {
    /**
     * The recently played tracks.
     */
    val recent: StateFlow<List<Track>?>
    val carousel: StateFlow<Long>
    val newlyAdded: StateFlow<List<Audio>?>

    /**
     * Callback method invoked upon clicking a history item.
     *
     * This method manages diverse scenarios contingent on the selected history item:
     *
     * - Scenario 1: In the event the newly added file is already present in the playback queue,
     *   the queue will be directed to the chosen item's position, initiating playback from there.
     *
     * - Scenario 2: If the recently added item is absent from the queue, the ensuing sub-scenarios arise:
     *   - Sub-Scenario 2.1: A Snackbar is exhibited to the user, offering options to either append
     *     the item to the current queue at next or substitute the queue with this recently added playlist.
     *   - Sub-Scenario 2.2: Further actions are contingent upon the user's decision.
     *
     * @param uri The URI of the history item clicked by the user.
     */
    fun onClickRecentFile(uri: String)

    /**
     * Callback method triggered when a recently added  item is clicked.
     * This function handles the action where the recently added file is either included in the
     * playback queue or prompts the user to replace the existing queue with recently added items.
     *
     * @param id The unique identifier of the recently added history item.
     */
    fun onClickRecentAddedFile(id: Long)

    /**
     * Callback method for handling new link input.
     */
    fun onNewLink(link: String)

    /**
     * Callback method invoked when the user requests the removal of a recent item.
     *
     * This function initiates the process of removing a specified item from the list
     * of recently played or accessed media.
     *
     * @param uri The URI of the recent item to be removed.
     */
    fun onRequestRemoveRecentItem(uri: String)
}