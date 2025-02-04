/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 04-02-2025.
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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.prime.media.R
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.menu.Action
import com.prime.media.common.raw
import com.prime.media.local.DirectoryViewState
import com.zs.core.store.Album
import com.zs.core.store.Genre
import com.zs.core.store.MediaProvider
import com.zs.core_ui.toast.Toast
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

private const val TAG = "AbstractLocalDirViewMod"

abstract class AbstractLocalDirViewModel<T>(
    val provider: MediaProvider
) : KoinViewModel(), DirectoryViewState<T> {

    abstract suspend fun fetch(filter: Filter, query: String? = null): Mapped<T>
    override val query: TextFieldState = TextFieldState()
    abstract override var order: Filter

    override fun filter(ascending: Boolean, order: Action) {
        if (order == this.order.second && this.order.first == ascending) return
        this.order = ascending to order
    }

    override val data: StateFlow<Mapped<T>?> = combine(
        snapshotFlow(query::raw).onStart { emit(null) }.drop(1).debounce(300L),
        snapshotFlow(::order),
        transform = { query, filter -> query to filter }
    )
        // transform it
        .flatMapLatest { (query, filter) ->
            provider.observer(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
                fetch(filter, query)
            }
        }
        // catch any exceptions.
        .catch {
            val report = report(it.message ?: getText(R.string.msg_unknown_error))
            if (report == Toast.ACTION_PERFORMED) Firebase.crashlytics.recordException(it)
        }
        // make sure the flow is released after sometime.
        .stateIn(viewModelScope, WhileSubscribed(), null)
}

private val ORDER_BY_TITLE = Action(R.string.title, id = MediaStore.Audio.Albums.ALBUM)
private val ORDER_BY_NONE = Action(R.string.none, id = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER)
private val ORDER_BY_ARTIST = Action(R.string.artist, id = MediaStore.Audio.Albums.ARTIST)

private val Album.firstTitleChar
    inline get() = title.uppercase(Locale.ROOT)[0].toString()

class AlbumsViewModel(provider: MediaProvider) : AbstractLocalDirViewModel<Album>(provider) {

    override var order: Filter by mutableStateOf(true to ORDER_BY_TITLE)
    override val title: CharSequence = getText(R.string.albums)
    override val orders: List<Action> = listOf(ORDER_BY_NONE, ORDER_BY_TITLE, ORDER_BY_ARTIST)

    override suspend fun fetch(filter: Filter, query: String?): Mapped<Album> {
        val (ascending, order) = filter
        val albums = provider.fetchAlbums(query, order.id, ascending)
        return when (order) {
            ORDER_BY_TITLE -> albums.groupBy { it.firstTitleChar }
            ORDER_BY_ARTIST -> albums.groupBy { it.artist }
            else -> albums.groupBy { "" }
        }
    }
}

// Genres ViewModel.
private val GENRES_ORDER_BY_TITLE = Action(R.string.title, id = MediaStore.Audio.Genres.NAME)
private val GENRE_ORDER_BY_NONE = Action(R.string.none, id = MediaStore.Audio.Genres._ID)

private val Genre.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

class GenresViewModel(provider: MediaProvider) : AbstractLocalDirViewModel<Genre>(provider) {
    override var order: Filter by mutableStateOf(true to GENRES_ORDER_BY_TITLE)
    override val title: CharSequence = getText(R.string.genres)
    override val orders: List<Action> = listOf(GENRE_ORDER_BY_NONE, GENRES_ORDER_BY_TITLE)

    override suspend fun fetch(filter: Filter, query: String?): Mapped<Genre> {
        val (ascending, order) = filter
        val result = provider.fetchGenres(query, order.id, ascending)
        return when (order) {
            GENRES_ORDER_BY_TITLE -> result.groupBy { it.firstTitleChar }
            else -> result.groupBy { "" }
        }
    }
}
