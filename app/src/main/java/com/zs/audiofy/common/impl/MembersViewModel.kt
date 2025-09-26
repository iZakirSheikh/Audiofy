/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 09-06-2025.
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

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Filter
import com.zs.audiofy.common.Mapped
import com.zs.audiofy.common.compose.FilterDefaults
import com.zs.audiofy.common.compose.FilterDefaults.ORDER_BY_TITLE
import com.zs.audiofy.common.compose.FilterDefaults.ORDER_NONE
import com.zs.audiofy.common.compose.directory.MetaData
import com.zs.audiofy.common.raw
import com.zs.audiofy.playlists.members.MembersViewState
import com.zs.audiofy.playlists.members.RouteMembers
import com.zs.audiofy.playlists.members.get
import com.zs.audiofy.settings.AppConfig
import com.zs.compose.foundation.castTo
import com.zs.core.common.debounceAfterFirst
import com.zs.core.db.playlists.Playlist
import com.zs.core.db.playlists.Playlist.Track
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.Remote
import com.zs.core.playback.toMediaFile
import com.zs.preferences.Key
import com.zs.preferences.stringPreferenceKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import java.util.Locale

private const val TAG = "MembersViewModel"

private val Track.firstTitleChar
    inline get() = title.uppercase(Locale.ROOT)[0].toString()

private val ACTION_REMOVE =
    Action(R.string.remove, id = "remove", icon = Icons.Outlined.PlaylistRemove)

class MembersViewModel(
    handle: SavedStateHandle, playlists: Playlists, remote: Remote
) : FilesViewModel<Track>(remote, playlists), MembersViewState {

    override val Track.key: Long get() = this.id
    val playlistName = handle[RouteMembers]!!
    override val orders: List<Action> = listOf(ORDER_NONE, ORDER_BY_TITLE,/* ORDER_BY_DATE_MODIFIED*/)

    override val showFavButton: Boolean get() = playlistName != Remote.PLAYLIST_FAVOURITE

    override var info: MetaData = let {
        val playlist = runBlocking { playlists[playlistName] }
        MetaData(
            if (playlistName == Remote.PLAYLIST_FAVOURITE) getText(R.string.scr_members_liked_playlist_title) else playlist?.name ?:"",
            null,
            playlist?.artwork?.toUri(),
            playlist?.count ?: -1,
            playlist?.dateModified ?: -1L
        )
    }

    override val filterKey: Key.Key2<String, Filter?> = stringPreferenceKey(
        "playlist_filter", null, FilterDefaults.FilterSaver { id ->
            when (id) {
                ORDER_NONE.id -> ORDER_NONE
               // ORDER_BY_DATE_MODIFIED.id -> ORDER_BY_DATE_MODIFIED
                ORDER_BY_TITLE.id -> ORDER_BY_TITLE
                else -> error("Unknown order passed. $id")
            }
        })

    override var filter: Filter by mutableStateOf(
        preferences[filterKey] ?: FilterDefaults.NO_FILTER
    )

    override val actions: List<Action> by derivedStateOf {
        buildList {
            this += ACTION_ADD_TO_PLAYLIST
            this += ACTION_PLAY_NEXT
            this += ACTION_ADD_TO_QUEUE
            this += ACTION_REMOVE
            if (!allSelected) this += ACTION_SELECT_ALL
        }
    }

    val observable = combine(
        flow = snapshotFlow(query::raw),
        flow2 = snapshotFlow(::filter),
        transform = { query, filter ->
            Triple(query, filter.first, filter.second)
        }).debounceAfterFirst(300).flatMapLatest { (query, ascending, order) ->
        playlists.observer(playlistName, query).debounceAfterFirst(300L).map { tracks ->
            when (order) {
                ORDER_NONE -> tracks.groupBy { "" }
                ORDER_BY_TITLE -> tracks.sortedBy { if (AppConfig.isFileGroupingEnabled) it.firstTitleChar else "" }
                    .let { if (ascending) it else it.reversed() }
                    .groupBy { if (AppConfig.isFileGroupingEnabled) it.firstTitleChar else "" }

                else -> error("Oops!! invalid order passed $order")
            }
        }
    }.onEach {
        data = castTo(it) as Mapped<Track>
    }.catch { exception ->
        Log.d(TAG, "provider: ${exception.stackTraceToString()}")
        val action = report(exception.message ?: getText(R.string.msg_unknown_error))
    }

    init {
        observable.launchIn(viewModelScope)
    }

    override fun play(item: Track?) {
        runCatching {
            val items = consume()
            // Must not happen.
            if (items.isEmpty())
                error("Error - Playable items must not be empty.")
            val index = if (item == null) -1 else items.indexOf(item)
            play(items.map(Track::toMediaFile), index, false)
        }
    }

    override fun shuffle() {
        runCatching {
            val items = consume()
            if (items.isEmpty())
                error("Error - Playable items must not be empty.")
            play(items.map(Track::toMediaFile), -1, true)
        }
    }

    fun remove(vararg item: Track) {
        runCatching {
            val id = playlistz[playlistName]?.id ?: return@runCatching
            val uris = item.map(Track::uri).toTypedArray()
            val count = playlistz.remove(id, *uris)

            val message = when {
                count == 0 -> "No tracks were removed — they may not be in the playlist."
                count < item.size -> "Removed $count of ${item.size} tracks from the playlist."
                else -> "All selected tracks were removed from the playlist."
            }

            showPlatformToast(message)
        }
    }


    override fun onPerformAction(value: Action, focused: Track?) {
        when (value) {
            ACTION_SELECT_ALL -> selectAll()
            ACTION_PLAY_NEXT if focused != null -> playNext(listOf(focused.toMediaFile()))
            ACTION_PLAY_NEXT -> playNext(consume().map(Track::toMediaFile))
            ACTION_ADD_TO_QUEUE if focused != null -> addToQueue(listOf(focused.toMediaFile()))
            ACTION_ADD_TO_QUEUE -> addToQueue(consume().map(Track::toMediaFile))
            ACTION_REMOVE if focused != null -> remove(focused)
            ACTION_REMOVE -> remove(*consume().toTypedArray())
            else -> showPlatformToast("Currently, ${getText(value.label)} isn’t supported, but we’re on it!")
        }
    }

    override fun toggleLiked(value: Track) {
        this@MembersViewModel.runCatching {
            val playlistId = playlistz[Remote.PLAYLIST_FAVOURITE]?.id
                ?: playlistz.insert(Playlist(Remote.PLAYLIST_FAVOURITE, ""))
            if (playlistz.contains(Remote.PLAYLIST_FAVOURITE, value.uri))
                playlistz.remove(playlistId, value.uri)
            else {
                val newTrack =  value.copy(playlistId, playlistz.lastPlayOrder(Remote.PLAYLIST_FAVOURITE) + 1, id = 0)
                playlistz.insert(listOf(newTrack))
            }
            showPlatformToast("Favourite list updated.")
        }
    }

    override fun addToPlaylist(playlistID: Long, track: Track?) {
        runCatching {
            val items = track?.let { listOf(it) } ?: consume().also { items ->
                require(items.isNotEmpty()) { "Error - Playable items must not be empty." }
            }

            val playlist = playlistz[playlistID]
                ?: error("Playlist not found for id=$playlistID")

            val filtered = items.filter {
                !playlistz.contains(playlist.name, it.key)
            }

            var lastPlayOrder = playlistz.lastPlayOrder(playlist.name)
            val tracks = filtered.map {  item ->
                // create new because we are doing this from one playlist to another.
                item.copy(playlistID, ++lastPlayOrder, id = 0)
            }

            val ids = playlistz.insert(tracks)
            showPlatformToast("${ids.size}/${items.size} items added to playlist.")
        }
    }
}