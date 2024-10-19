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

package com.prime.media.playlists

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.AddToQueue
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueuePlayNext
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.lifecycle.SavedStateHandle
import com.prime.media.R
import com.prime.media.common.MetaData
import com.prime.media.common.Route
import com.prime.media.common.SafeArgs
import com.prime.media.common.menu.Action
import com.zs.core.db.Playlist
import com.zs.core.db.Playlist.Track
import kotlinx.coroutines.flow.Flow

/**
 * Represents the route to all playlist.
 */
object RoutePlaylists : Route

interface PlaylistsViewState {

    val data: Flow<List<Playlist>>

    fun create(value: Playlist)

    fun delete(playlist: Playlist)

    fun update(playlist: Playlist)
}


private const val PRAM_MEMBER_KEY = "pram_member_key"

/**
 * Represents the route to all playlist members identified by `name` or `id`.
 */
object RoutePlaylist : SafeArgs<String> {
    override val route: String = "$domain/{${PRAM_MEMBER_KEY}}"

    override fun invoke(name: String): String = "$domain/${name}"

    operator fun invoke(id: Long): String = invoke("$id")

    override fun build(handle: SavedStateHandle): String = handle[PRAM_MEMBER_KEY]!!
}

/**
 * Represents the view state of a playlist.
 *
 * It provides access to the playlist metadata, tracks, available actions,
 * search query, order options, and current filter.
 *
 * @property focused The key of the currently focused item, if any.
 * @property metadata Metadata about the playlist, such as title, description, and artwork.
 * @property data A flow emitting a map of track groups, keyed by header.
 * The flow can emit null if data is not available.
 * @property actions A list of supported actions for the playlist, such as play, shuffle,
 * remove, add to queue, and add to playlist. These actions are available
 * when an item is clicked or multiple items are selected.
 * @property query The state of the search query, represented as a `TextFieldState`.
 * @property filter A pair representing the current filter: the first element is a boolean
 * @property orders A list of available order options, each represented by an `Action`.
 * indicating ascending/descending order, and the second element is the filter action.
 */
interface PlaylistViewState {

    companion object {
        // Actions
        val ACTION_PLAY = Action(R.string.play, Icons.Rounded.PlayArrow)
        val ACTION_SHUFFLE = Action(R.string.shuffle, Icons.Rounded.Shuffle)
        val ACTION_REMOVE = Action(R.string.remove, Icons.Outlined.RemoveCircle)
        val ACTION_PLAY_NEXT = Action(R.string.play_next, Icons.Outlined.QueuePlayNext)
        val ACTION_ADD_TO_QUEUE = Action(R.string.add_to_queue, Icons.Outlined.AddToQueue)
        val ACTION_ADD_TO_PLAYLIST = Action(R.string.add_to_playlist, Icons.Outlined.PlaylistAdd)
        val ACTION_DELETE_PLAYLIST =
            Action(R.string.scr_playlist_delete, Icons.AutoMirrored.Outlined.PlaylistAdd)
        val ACTION_SEARCH = Action(R.string.search, Icons.Outlined.Search)
    }

    var focused: String?
    val metadata: MetaData?
    val data: Flow<Map<String, List<Track>>?>
    val actions: List<Action>
    val query: TextFieldState
    val filter: Pair<Boolean, Action>
    val orders: List<Action>

    /**
     * Filters the playlist items based on the given query, order, and ascending/descending flag.
     *
     * @param ascending True for ascending order, false for descending order.
     * @param order The action representing the order to apply.
     */
    fun filter(ascending: Boolean = this.filter.first, order: Action = this.filter.second)

    /**
     * Starts playback of the playlist, respecting the current filter, query, and shuffle settings.
     * Playback starts from the focused item if it is not null, otherwise from the first item.
     *
     * @param shuffle If true, shuffles the playlist before starting playback.
     */
    fun play(shuffle: Boolean = false)

    /**
     * Adds the currently focused track to the playlist represented by the given key.
     *
     * @param key The key representing the playlist.
     */
    fun addToPlaylist(key: String)

    /**
     * Removes the currently focused track from the playlist.
     */
    fun remove()

    /**
     * Adds the currently focused item to the end of the queue.
     * If the queue is empty, the item is added at the first index.
     */
    fun addToQueue()

    /**
     * Adds the currently focused item to the queue at the next index.
     * If the queue is empty, the item is added at the first index.
     */
    fun playNext()

    /**
     * Requests to delete the playlist.
     */
    fun delete()
}
