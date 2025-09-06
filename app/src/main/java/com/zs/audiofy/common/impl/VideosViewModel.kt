/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 03-06-2025.
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

import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Filter
import com.zs.audiofy.common.compose.FilterDefaults
import com.zs.audiofy.common.compose.directory.MetaData
import com.zs.audiofy.common.ellipsize
import com.zs.audiofy.videos.RouteVideos
import com.zs.audiofy.videos.VideosViewState
import com.zs.audiofy.videos.get
import com.zs.core.common.PathUtils
import com.zs.core.common.toMediaFile
import com.zs.core.common.toTrack
import com.zs.core.db.playlists.Playlist
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.Remote
import com.zs.core.store.MediaProvider
import com.zs.core.store.models.Video
import com.zs.preferences.Key
import com.zs.preferences.stringPreferenceKey
import kotlinx.coroutines.flow.launchIn
import java.util.Locale

private val Video.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

class VideosViewModel(
    handle: SavedStateHandle,
    remote: Remote,
    playlists: Playlists,
    private val provider: MediaProvider,
) : StoreViewModel<Video>(provider, remote, playlists), VideosViewState {

    override val contentUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    override val Video.key: Long get() = this.id

    val parent = handle[RouteVideos]
    override val filterKey: Key.Key2<String, Filter?> = stringPreferenceKey(
        if (parent == null) "video_library_filter" else "video_folder_filter",
        null,
        FilterDefaults.FilterSaver { id ->
            when (id) {
                ORDER_BY_DATE_MODIFIED.id -> ORDER_BY_DATE_MODIFIED
                ORDER_BY_TITLE.id -> ORDER_BY_TITLE
                else -> ORDER_BY_NONE
            }
        }
    )

    override var filter: Filter by mutableStateOf(
        preferences[filterKey] ?: FilterDefaults.NO_FILTER
    )

    private val Action.toAndroidOrder
        get() = when (this.id) {
            ORDER_BY_DATE_MODIFIED.id -> MediaProvider.COLUMN_DATE_MODIFIED
            ORDER_BY_TITLE.id -> MediaProvider.COLUMN_NAME
            else -> MediaStore.Video.Media.DEFAULT_SORT_ORDER
        }

    override val orders: List<Action> = buildList {
        this += ORDER_BY_NONE
        this += ORDER_BY_DATE_MODIFIED
        this += ORDER_BY_TITLE
    }

    override var info: MetaData by mutableStateOf(
        when (parent) {
            null -> MetaData(getText(R.string.scr_videos_title))
            else -> MetaData(PathUtils.name(parent).ellipsize(12), PathUtils.parent(parent))
        },
    )

    override val actions: List<Action> by derivedStateOf {
        buildList {
            this += ACTION_ADD_TO_PLAYLIST
            this += ACTION_PLAY_NEXT
            this += ACTION_ADD_TO_QUEUE
            this += ACTION_SHARE
            this += ACTION_DELETE
            if (!allSelected) this += ACTION_SELECT_ALL
            if (!isInSelectionMode) {
                this += ACTION_INFO
            }
        }
    }

    override suspend fun refresh(query: String?, ascending: Boolean, order: Action) {
        val order = order.toAndroidOrder
        val files = provider.fetchVideoFiles(query, order, ascending, parent)
        // Emit only if query is null or empty
        if (query.isNullOrBlank()) {
            val latest = files.maxByOrNull { it.dateModified }
            info = info.copy(
                dateModified = latest?.dateModified ?: -1,
                artwork = latest?.contentUri,
                cardinality = files.size
            )
        }
        // Group data.
        data = when (filter.second) {
            ORDER_BY_NONE -> files.groupBy { "" }
            ORDER_BY_TITLE -> files.groupBy { it.firstTitleChar }
            ORDER_BY_DATE_MODIFIED -> files.groupBy {
                val mills = System.currentTimeMillis()
                DateUtils.getRelativeTimeSpanString(
                    /* time = */ it.dateModified,
                    /* now = */ mills,
                    /* minResolution = */ DateUtils.DAY_IN_MILLIS
                )
            }
            // report unknown.
            else -> error("Invalid order passed. $order")
        }
    }

    init { flow.launchIn(viewModelScope) }

    override fun play(item: Video?) {
        runCatching {
            val items = consume()
            if (items.isEmpty())
                error("Error - Playable items must not be empty.")
            val index = if (item == null) -1 else items.indexOf(item)
            play(items.map(Video::toMediaFile), index, false)
        }
    }

    override fun shuffle() {
        runCatching {
            val items = consume()
            if (items.isEmpty())
                error("Error - Playable items must not be empty.")
            play(items.map(Video::toMediaFile), -1, true)
        }
    }

    override fun onPerformAction(value: Action, resolver: Activity, focused: Video?) {
        when(value){
            ACTION_PLAY_NEXT if (focused != null) -> playNext(listOf(focused.toMediaFile()))
            ACTION_PLAY_NEXT -> playNext(consume().map(Video::toMediaFile))
            ACTION_ADD_TO_QUEUE if (focused != null) -> addToQueue(listOf(focused.toMediaFile()))
            ACTION_ADD_TO_QUEUE -> addToQueue(consume().map(Video::toMediaFile))
            ACTION_SHARE if (focused != null) -> share(resolver, focused.id)
            ACTION_SHARE -> share(resolver, *consume().map(Video::id).toLongArray())
            ACTION_DELETE if (focused != null) -> remove(resolver, focused.id)
            ACTION_DELETE -> remove(resolver, *consume().map(Video::id).toLongArray())
            ACTION_SELECT_ALL -> selectAll()
            else -> showPlatformToast("Currently, ${getText(value.label)} isn’t supported, but we’re on it!")
        }
    }

    override fun toggleLiked(value: Video) {
        runCatching {
            val playlistId = playlistz[Remote.PLAYLIST_FAVOURITE]?.id
                ?: playlistz.insert(Playlist(Remote.PLAYLIST_FAVOURITE, ""))
            if (playlistz.contains(Remote.PLAYLIST_FAVOURITE, value.contentUri.toString()))
                playlistz.remove(playlistId, value.contentUri.toString())
            else {
                val newTrack = value.toTrack(playlistId, playlistz.lastPlayOrder(Remote.PLAYLIST_FAVOURITE) + 1)
                playlistz.insert(listOf(newTrack))
            }
            showPlatformToast("Favourite list updated.")
        }
    }
}