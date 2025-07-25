/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 23-05-2025.
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.R
import com.zs.audiofy.audios.AudiosViewState
import com.zs.audiofy.audios.RouteAudios
import com.zs.audiofy.audios.RouteAudios.SOURCE_ALBUM
import com.zs.audiofy.audios.RouteAudios.SOURCE_ALL
import com.zs.audiofy.audios.RouteAudios.SOURCE_ARTIST
import com.zs.audiofy.audios.RouteAudios.SOURCE_FOLDER
import com.zs.audiofy.audios.RouteAudios.SOURCE_GENRE
import com.zs.audiofy.audios.get
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.EDIT
import com.zs.audiofy.common.Filter
import com.zs.audiofy.common.GO_TO_ALBUM
import com.zs.audiofy.common.compose.FilterDefaults
import com.zs.audiofy.common.compose.directory.MetaData
import com.zs.audiofy.common.ellipsize
import com.zs.core.common.PathUtils
import com.zs.core.common.toTrack
import com.zs.core.db.playlists.Playlist
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.Remote
import com.zs.core.playback.toMediaFile
import com.zs.core.store.MediaProvider
import com.zs.core.store.models.Audio
import com.zs.preferences.Key
import com.zs.preferences.stringPreferenceKey
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "AudiosViewModel"

private val Audio.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

private fun MediaProvider.getArtistName(id: Long) = runBlocking { getArtist(id) }.name.ellipsize(12)
private fun MediaProvider.getAlbumName(id: Long) = runBlocking { getAlbum(id) }.title.ellipsize(12)
private fun MediaProvider.getGenreName(id: Long) = runBlocking { getGenre(id) }.name.ellipsize(12)

private val ORDER_BY_ALBUM = Action(R.string.album, id = "filter_by_album")
private val ORDER_BY_ARTIST get() = Action(R.string.artist, id = "filter_by_artist")

//
private val ACTION_EDIT = Action.EDIT
private val ACTION_GO_TO_ALBUM = Action.GO_TO_ALBUM

class AudiosViewModel(
    handle: SavedStateHandle,
    remote: Remote,
    playlists: Playlists,
    private val provider: MediaProvider
) : StoreViewModel<Audio>(provider, remote, playlists), AudiosViewState {


    override val Audio.key: Long get() = this.id

    val _args = handle[RouteAudios]
    val source = _args.first;
    val extra = _args.second

    private val Action.toAndroidOrder
        get() = when (this.id) {
            ORDER_BY_DATE_MODIFIED.id -> MediaProvider.COLUMN_DATE_MODIFIED
            ORDER_BY_TITLE.id -> MediaProvider.COLUMN_NAME
            ORDER_BY_ALBUM.id -> MediaProvider.COLUMN_AUDIO_ALBUM
            ORDER_BY_ARTIST.id -> MediaProvider.COLUMN_AUDIO_ARTIST
            ORDER_BY_LENGTH.id -> MediaProvider.COLUMN_MEDIA_DURATION
            else -> MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        }

    override val contentUri: Uri = when (source) {
        SOURCE_ALL, SOURCE_FOLDER -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        SOURCE_ALBUM -> MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        SOURCE_ARTIST -> MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
        SOURCE_GENRE -> MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
        else -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    override val filterKey: Key.Key2<String, Filter?> = stringPreferenceKey(
        "${source}_filter",
        null,
        FilterDefaults.FilterSaver { id ->
            when (id) {
                ORDER_BY_DATE_MODIFIED.id -> ORDER_BY_DATE_MODIFIED
                ORDER_BY_TITLE.id -> ORDER_BY_TITLE
                ORDER_BY_ALBUM.id -> ORDER_BY_ALBUM
                ORDER_BY_ARTIST.id -> ORDER_BY_ARTIST
                ORDER_BY_LENGTH.id -> ORDER_BY_LENGTH
                else -> ORDER_BY_NONE
            }
        }
    )

    override var filter: Filter by mutableStateOf(
        preferences[filterKey] ?: FilterDefaults.NO_FILTER
    )

    override val orders: List<Action> = buildList {
        this += ORDER_BY_NONE
        this += ORDER_BY_DATE_MODIFIED
        this += ORDER_BY_TITLE
        this += ORDER_BY_LENGTH
        if (source != SOURCE_ALBUM)
            this += ORDER_BY_ALBUM
        if (source != SOURCE_ARTIST)
            ORDER_BY_ARTIST
    }

    override val actions: List<Action> by derivedStateOf {
        buildList {
            this += ACTION_ADD_TO_PLAYLIST
            this += ACTION_PLAY_NEXT
            this += ACTION_ADD_TO_QUEUE
            if (!isInSelectionMode && source != SOURCE_ALBUM)
                this += ACTION_GO_TO_ALBUM
//            if (!isInSelectionMode && source != SOURCE_ALBUM)
//                this += ACTION_GO_TO_ARTIST
            if (!isInSelectionMode)
                this += ACTION_EDIT
            this += ACTION_SHARE
            this += ACTION_DELETE
            if (!allSelected) this += ACTION_SELECT_ALL
            if (!isInSelectionMode) {
                this += ACTION_INFO
            }
        }
    }

    override var info: MetaData by mutableStateOf(
        when (source) {
            SOURCE_ALL -> MetaData(getText(R.string.scr_audios_title), icon = Icons.Outlined.LibraryBooks)

            SOURCE_FOLDER -> MetaData(PathUtils.name(extra!!).ellipsize(12), extra)
            SOURCE_ARTIST -> MetaData(
                provider.getArtistName(extra!!.toLong()),
                getText(R.string.artist)
            )

            SOURCE_ALBUM -> MetaData(
                provider.getAlbumName(extra!!.toLong()),
                getText(R.string.album)
            )

            SOURCE_GENRE -> MetaData(
                provider.getGenreName(extra!!.toLong()),
                getText(R.string.genre)
            )

            else -> error("$TAG unknown source: $source")
        }
    )

    override suspend fun refresh(query: String?, ascending: Boolean, order: Action) {
        val order = order.toAndroidOrder
        val files = with(provider) {
            when (source) {
                SOURCE_ALL, SOURCE_FOLDER -> fetchAudioFiles(query, order, ascending, extra)
                SOURCE_ARTIST -> fetchArtistAudios(
                    extra!!.toLong(),
                    order = order,
                    ascending = ascending
                )

                SOURCE_ALBUM -> fetchAlbumAudios(
                    extra!!.toLong(),
                    order = order,
                    ascending = ascending
                )

                SOURCE_GENRE -> fetchGenreAudios(
                    extra!!.toLong(),
                    order = order,
                    ascending = ascending
                )

                else -> TODO("$source not implemented yet!")
            }
        }
        // Emit only if query is null or empty
        if (query.isNullOrBlank()) {
            val latest = files.maxByOrNull { it.dateModified }
            info = info.copy(
                dateModified = latest?.dateModified ?: -1,
                artwork = MediaProvider.buildAlbumArtUri(latest?.albumId ?: -1L),
                cardinality = files.size
            )
        }
        // Group data.
        data = when (filter.second) {
            ORDER_BY_NONE -> files.groupBy { "" }
            ORDER_BY_TITLE -> files.groupBy { it.firstTitleChar }
            ORDER_BY_ALBUM -> files.groupBy { it.album }
            ORDER_BY_ARTIST -> files.groupBy { it.artist }
            ORDER_BY_DATE_MODIFIED -> files.groupBy {
                val mills = System.currentTimeMillis()
                DateUtils.getRelativeTimeSpanString(
                    /* time = */ it.dateModified,
                    /* now = */ mills,
                    /* minResolution = */ DateUtils.DAY_IN_MILLIS
                )
            }

            ORDER_BY_LENGTH -> files.groupBy { audio ->
                when {
                    audio.duration < TimeUnit.MINUTES.toMillis(2) -> getText(R.string.duration_under_2_min)
                    audio.duration < TimeUnit.MINUTES.toMillis(5) -> getText(R.string.duration_under_5_min)
                    audio.duration < TimeUnit.MINUTES.toMillis(10) -> getText(R.string.duration_under_10_min)
                    else -> getText(R.string.duration_over_10_min)
                }
            }
            // groupby length
            else -> error("Invalid order passed. $order")
        }
    }

    init {
        flow.launchIn(viewModelScope)
    }

    override fun play(item: Audio?) {
        runCatching {
            val items = consume()
            if (items.isEmpty())
                error("Error - Playable items must not be empty.")
            val index = if (item == null) -1 else items.indexOf(item)
            play(items.map(Audio::toMediaFile), index, false)
        }
    }

    override fun shuffle() {
        runCatching {
            val items = consume()
            if (items.isEmpty())
                error("Error - Playable items must not be empty.")
            play(items.map(Audio::toMediaFile), -1, true)
        }
    }

    override fun toggleLiked(value: Audio) {
        runCatching {
            val playlistId = playlists[Remote.PLAYLIST_FAVOURITE]?.id
                ?: playlists.insert(Playlist(Remote.PLAYLIST_FAVOURITE, ""))
            if (playlists.contains(Remote.PLAYLIST_FAVOURITE, value.uri.toString()))
                playlists.remove(playlistId, value.uri.toString())
            else {
                val newTrack = value.toTrack(playlistId, playlists.lastPlayOrder(Remote.PLAYLIST_FAVOURITE) + 1)
                playlists.insert(listOf(newTrack))
            }
            showPlatformToast("Favourite list updated.")
        }
    }

    override fun onPerformAction(value: Action, resolver: Activity, focused: Audio?) {
        when(value){
            ACTION_PLAY_NEXT if (focused != null) -> playNext(listOf(focused.toMediaFile()))
            ACTION_PLAY_NEXT -> playNext(consume().map(Audio::toMediaFile))
            ACTION_ADD_TO_QUEUE if (focused != null) -> addToQueue(listOf(focused.toMediaFile()))
            ACTION_ADD_TO_QUEUE -> addToQueue(consume().map(Audio::toMediaFile))
            ACTION_SHARE if (focused != null) -> share(resolver, focused.id)
            ACTION_SHARE -> share(resolver, *consume().map(Audio::id).toLongArray())
            ACTION_DELETE if (focused != null) -> remove(resolver, focused.id)
            ACTION_DELETE -> remove(resolver, *consume().map(Audio::id).toLongArray())
            ACTION_SELECT_ALL -> selectAll()
            else -> showPlatformToast("Currently, ${getText(value.label)} isn’t supported, but we’re on it!")
        }
    }
}