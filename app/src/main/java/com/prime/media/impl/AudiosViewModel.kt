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

package com.prime.media.impl

import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.media.R
import com.prime.media.audios.AudiosViewState
import com.prime.media.audios.RouteAudios
import com.prime.media.audios.RouteAudios.SOURCE_ALBUM
import com.prime.media.audios.RouteAudios.SOURCE_ALL
import com.prime.media.audios.RouteAudios.SOURCE_ARTIST
import com.prime.media.audios.RouteAudios.SOURCE_FOLDER
import com.prime.media.audios.RouteAudios.SOURCE_GENRE
import com.prime.media.audios.get
import com.prime.media.common.Action
import com.prime.media.common.EDIT
import com.prime.media.common.Filter
import com.prime.media.common.GO_TO_ALBUM
import com.prime.media.common.GO_TO_ARTIST
import com.prime.media.common.INFO
import com.prime.media.common.compose.FilterDefaults
import com.prime.media.common.compose.directory.MetaData
import com.prime.media.common.debounceAfterFirst
import com.prime.media.common.raw
import com.zs.core.store.MediaProvider
import com.zs.core.store.models.Audio
import com.zs.preferences.Key
import com.zs.preferences.stringPreferenceKey
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale

private const val TAG = "AudiosViewModel"

private val ORDER_BY_NONE = FilterDefaults.ORDER_NONE
private val ORDER_BY_TITLE = FilterDefaults.ORDER_BY_TITLE
private val ORDER_BY_DATE_MODIFIED = FilterDefaults.ORDER_BY_DATE_MODIFIED
private val ORDER_BY_ALBUM = Action(R.string.album, id = "filter_by_album")
private val ORDER_BY_ARTIST get() = Action(R.string.artist, id = "filter_by_artist")
private val ORDER_BY_LENGTH get() = Action(R.string.length, id = "filter_by_length")

private val Audio.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

private val Action.toAndroidOrder
    get() = when (this.id) {
        ORDER_BY_DATE_MODIFIED.id -> MediaProvider.COLUMN_DATE_MODIFIED
        ORDER_BY_TITLE.id -> MediaProvider.COLUMN_NAME
        ORDER_BY_ALBUM.id -> MediaProvider.COLUMN_AUDIO_ALBUM
        ORDER_BY_ARTIST.id -> MediaProvider.COLUMN_AUDIO_ARTIST
        ORDER_BY_LENGTH.id -> MediaProvider.COLUMN_MEDIA_DURATION
        else -> MediaStore.Audio.Media.DEFAULT_SORT_ORDER
    }

private infix fun CharSequence.with(extra: CharSequence? = null) = buildAnnotatedString {
    append(this@with)
    if (extra == null) return@buildAnnotatedString
    withStyle(SpanStyle(color = Color.Gray, fontSize = 11.sp)) {
        append("\n$extra")
    }
}

// actions
private val ADD_TO_PLAYLIST = Action(R.string.add_to_playlist, Icons.Outlined.PlaylistAdd)
private val PLAY_NEXT = Action(R.string.play_next, Icons.Outlined.SkipNext)
private val ADD_TO_QUEUE = Action(R.string.add_to_queue, Icons.AutoMirrored.Outlined.QueueMusic)
private val GO_TO_ARTIST = Action.GO_TO_ARTIST
private val GO_TO_ALBUM = Action.GO_TO_ALBUM
private val DELETE = Action(R.string.delete, Icons.Default.DeleteOutline)
private val SHARE = Action(R.string.share, Icons.Outlined.Share)
private val SELECT_ALL = Action(R.string.select_all, Icons.Outlined.SelectAll)
private val INFO = Action.INFO
private val EDIT = Action.EDIT


class AudiosViewModel(
    handle: SavedStateHandle,
    private val provider: MediaProvider
) : FilesViewModel<Audio>(), AudiosViewState {

    val _args = handle[RouteAudios]
    val source = _args.first;
    val extra = _args.second

    override var info: MetaData by mutableStateOf(
        MetaData(
            when (source) {
                SOURCE_ALL -> getText(R.string.audio_library_title)
                SOURCE_FOLDER -> getText(R.string.folder) with "$extra"
                SOURCE_ARTIST -> getText(R.string.artist) with "name"
                SOURCE_ALBUM -> getText(R.string.album) with "name"
                SOURCE_GENRE -> getText(R.string.genre) with "name"
                else -> error("$TAG unknown source: $source")
            }
        )
    )

    override val Audio.id: Long get() = this.id

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
            this += ADD_TO_PLAYLIST
            this += PLAY_NEXT
            this += ADD_TO_QUEUE
            if (!isInSelectionMode && source != SOURCE_ALBUM)
                this += GO_TO_ALBUM
            if (!isInSelectionMode && source != SOURCE_ALBUM)
                this += GO_TO_ARTIST
            this += SHARE
            this += DELETE
            if (!allSelected) this += SELECT_ALL
            if (!isInSelectionMode) {
                this += INFO
                this += EDIT
            }
        }
    }

    override fun play(from: Audio?) {
        TODO("Not yet implemented")
    }

    override fun shuffle() {
        TODO("Not yet implemented")
    }

    private suspend fun fetch(
        query: String?,
        ascending: Boolean,
        order: String
    ): Unit {

        val files = when (source) {
            SOURCE_ALL, SOURCE_FOLDER -> provider.fetchAudioFiles(query, order, ascending, extra)
            SOURCE_ARTIST -> provider.fetchArtistAudios(
                extra!!.toLong(),
                order = order,
                ascending = ascending,
            )

            SOURCE_ALBUM -> provider.fetchAlbumAudios(
                extra!!.toLong(),
                order = order,
                ascending = ascending,
            )
            // SOURCE_GENRE -> provider.fetchGenreAudios(extra!!.toLong(), order =order, ascending = ascending, )
            else -> TODO("$source not implemented yet!")
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
        data = when(filter.second){
            ORDER_BY_ALBUM -> files.groupBy { it.album }
            ORDER_BY_ARTIST -> files.groupBy { it.artist }
            ORDER_BY_LENGTH -> TODO("$TAG ${filter.second} not Implemented yet!.")
            ORDER_BY_TITLE -> files.groupBy { it.firstTitleChar }
            ORDER_BY_DATE_MODIFIED -> files.groupBy {
                val mills = System.currentTimeMillis()
                DateUtils.getRelativeTimeSpanString(it.dateModified, mills, DateUtils.DAY_IN_MILLIS)
            }
            ORDER_BY_NONE -> files.groupBy { "" }
            else -> files.groupBy { it.firstTitleChar }
        }
    }

    init {
        //
        combine(
            flow = provider.observer(MediaProvider.EXTERNAL_AUDIO_URI),
            flow2 = snapshotFlow(query::raw),
            flow3 = snapshotFlow(::filter),
            transform = { _, query, filter -> Triple(query, filter.first, filter.second) }
        ).debounceAfterFirst(300)
            .onEach() { (query, ascending, order) -> fetch(query, ascending, order.toAndroidOrder) }
            .catch { exception ->
                Log.d(TAG, "provider: ${exception.stackTraceToString()}")
                val action = report(exception.message ?: getText(R.string.msg_unknown_error))
            }
            .launchIn(viewModelScope)
    }
}