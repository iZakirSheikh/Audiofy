@file:SuppressLint("BuildListAdds")
@file:OptIn(ExperimentalCoroutinesApi::class)

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

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Title
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.prime.media.R
import com.prime.media.common.MetaData
import com.prime.media.common.ellipsize
import com.prime.media.common.get
import com.prime.media.common.menu.Action
import com.prime.media.playlists.PlaylistViewState
import com.prime.media.playlists.RoutePlaylist
import com.zs.core.db.Playlist.Track
import com.zs.core.db.Playlists
import com.zs.core.playback.Playback
import com.zs.core_ui.toast.Toast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

private const val TAG = "PlaylistViewModel"

/**
 * @author Zakir Sheikh
 * @return flow based on the type of the key.
 */
private fun Playlists.observable(key: String, query: String? = null) =
    if (key.toLongOrNull() != null) observe(key.toLong(), query) else observer(key, query)

private val Track.firstTitleChar
    inline get() = title.uppercase()[0].toString()

// order by actions
private val ORDER_BY_TITLE =
    Action(R.string.title, Icons.Outlined.Title)
private val ORDER_BY_PLAY_ORDER =
    Action(R.string.play_order, Icons.Outlined.Shuffle)

// Typealias for representing these complex types.
private typealias Filter = Pair<Boolean, Action>
private typealias Grouped = Map<String, List<Track>>

private val SavedStateHandle.defaultFilter: Filter
    inline get() {
        val key = get(RoutePlaylist)
        return when {
            key == Playback.PLAYLIST_FAVOURITE -> true to ORDER_BY_TITLE
            else -> true to ORDER_BY_PLAY_ORDER
        }
    }

class PlaylistViewModel(
    handle: SavedStateHandle,
    private val playlists: Playlists
) : KoinViewModel(), PlaylistViewState {

    val key = handle[RoutePlaylist]
    override var focused: String? by mutableStateOf(null)
    override var metadata: MetaData? by mutableStateOf(null)

    // filters
    override val query: TextFieldState = TextFieldState()
    override var filter: Filter by mutableStateOf(handle.defaultFilter)

    // orders
    override val actions: List<Action> = buildList {
        this += PlaylistViewState.ACTION_PLAY_NEXT
        this += PlaylistViewState.ACTION_ADD_TO_QUEUE
        this += PlaylistViewState.ACTION_ADD_TO_PLAYLIST
        this += PlaylistViewState.ACTION_REMOVE
    }
    override val orders: List<Action> = buildList {
        // recent playlist is always order by play-order.
        if (key == Playback.PLAYLIST_RECENT)
            return@buildList
        this += ORDER_BY_TITLE
        this += ORDER_BY_PLAY_ORDER
    }

    override fun filter(ascending: Boolean, order: Action) {
        if (filter.first == ascending && filter.second == order) return
        filter = ascending to order
    }

    override fun play(shuffle: Boolean): Unit = TODO("Not yet implemented")
    override fun addToPlaylist(key: String): Unit = TODO("Not yet implemented")
    override fun remove(): Unit = TODO("Not yet implemented")
    override fun addToQueue(): Unit = TODO("Not yet implemented")
    override fun playNext(): Unit = TODO("Not yet implemented")
    override fun delete(): Unit = TODO("Not yet implemented")

    // why use stateflow; because i need cached items.
    //
    override val data: StateFlow<Grouped?> = /*combine(
        snapshotFlow(query::text).drop(1).debounce(300L),
        snapshotFlow(::filter)
    ) { query, filter -> query to filter }
        // emit a new flow of observables.
        .flatMapLatest { (query, filter) ->

        }*/
        playlists.observable(key, (query.toString()).ifEmpty { null }).map {
            it.groupBy { it.firstTitleChar }
        }
        // capture new meta data
        .onEach { metadata = buildMetaData() }
        // catch any errors.
        .catch {
            val report = report(it.message ?: getText(R.string.msg_unknown_error))
            if (report == Toast.ACTION_PERFORMED)
                Firebase.crashlytics.recordException(it)
        }
        // make sure the flow is released after sometime.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000L), null)

    suspend fun buildMetaData(): MetaData {
        val info = if (key.toLongOrNull() == null) playlists[key] else playlists[key.toLong()]
        if (info == null) {
            return MetaData("N/A")
        }
        return MetaData(
            info.name.ellipsize(14),
            artwork = info.artwork,
            desc = info.desc,
            options = buildList {
                this += PlaylistViewState.ACTION_PLAY
                this += PlaylistViewState.ACTION_SHUFFLE
                this += PlaylistViewState.ACTION_SEARCH
                this += PlaylistViewState.ACTION_DELETE_PLAYLIST
            },
            extra = buildList {
                this += getText(R.string.scr_playlist_files_d, info.count)
                this += getText(R.string.scr_playlist_date_modified_s, DateUtils.getRelativeTimeSpanString(info.dateModified))
            }
        )
    }
}