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

package com.prime.media.impl

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
import com.prime.media.R
import com.prime.media.common.Action
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.compose.FilterDefaults
import com.prime.media.common.compose.FilterDefaults.ORDER_BY_DATE_MODIFIED
import com.prime.media.common.compose.FilterDefaults.ORDER_BY_TITLE
import com.prime.media.common.compose.FilterDefaults.ORDER_NONE
import com.prime.media.common.compose.directory.MetaData
import com.zs.core.common.debounceAfterFirst
import com.prime.media.common.raw
import com.prime.media.playlists.members.MembersViewState
import com.prime.media.playlists.members.RouteMembers
import com.prime.media.playlists.members.get
import com.zs.compose.foundation.castTo
import com.zs.core.db.playlists.Playlist.Track
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.MediaFile
import com.zs.core.playback.Remote
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
    handle: SavedStateHandle, val playlists: Playlists, remote: Remote
) : FilesViewModel<Track>(remote), MembersViewState {

    override val Track.id: Long get() = this.id
    override val Track.asMediaFile: MediaFile
        get() = TODO("Not yet implemented")
    val playlistName = handle[RouteMembers]!!
    override val orders: List<Action> = listOf(ORDER_NONE, ORDER_BY_TITLE, ORDER_BY_DATE_MODIFIED)
    override var info: MetaData  = let {
        val playlist = runBlocking { playlists[playlistName] }
        when (playlistName) {
            Remote.PLAYLIST_FAVOURITE -> MetaData(
                getText(R.string.scr_members_favourite_playlist_title),
                null,
                playlist?.artwork?.toUri(),
                playlist?.count ?: -1,
                playlist?.dateModified ?: -1L
            )
            else -> MetaData(playlistName, )
        }
    }

    override val filterKey: Key.Key2<String, Filter?> = stringPreferenceKey(
        "playlist_filter", null, FilterDefaults.FilterSaver { id ->
            when (id) {
                ORDER_NONE.id -> ORDER_NONE
                ORDER_BY_DATE_MODIFIED.id -> ORDER_BY_DATE_MODIFIED
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
                        ORDER_BY_TITLE -> tracks.sortedBy { it.firstTitleChar }
                            .let { if (ascending) it else it.reversed() }
                            .groupBy { it.firstTitleChar }

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
}