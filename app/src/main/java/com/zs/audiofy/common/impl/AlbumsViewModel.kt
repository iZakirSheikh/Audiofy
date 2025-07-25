/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
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

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Filter
import com.zs.audiofy.common.Mapped
import com.zs.audiofy.common.compose.FilterDefaults
import com.zs.audiofy.common.compose.FilterDefaults.FilterSaver
import com.zs.core.store.MediaProvider
import com.zs.core.store.models.Audio.Album
import com.zs.preferences.stringPreferenceKey
import java.util.Locale

class AlbumsViewModel(provider: MediaProvider) : LocalDirectoryViewModel<Album>(provider) {

    private val ORDER_BY_TITLE = FilterDefaults.ORDER_BY_TITLE
    private val ORDER_BY_NONE = FilterDefaults.ORDER_NONE
    private val ORDER_BY_ARTIST = FilterDefaults.ORDER_BY_ARTIST

    val filterKey =
        stringPreferenceKey(
            "album_filter_key",
            null,
            FilterSaver {
                when (it) {
                    ORDER_BY_TITLE.id -> ORDER_BY_TITLE
                    ORDER_BY_ARTIST.id -> ORDER_BY_ARTIST
                    else -> ORDER_BY_NONE
                }
            }
        )

    override fun filter(ascending: Boolean, order: Action) {
        super.filter(ascending, order)
        preferences[filterKey] = ascending to order
    }

    override var filter: Filter by mutableStateOf(
        preferences[filterKey] ?: (true to ORDER_BY_TITLE)
    )

    override val uri: Uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
    private val Album.firstTitleChar
        inline get() = title.uppercase(Locale.ROOT)[0].toString()

    override val title: CharSequence = getText(R.string.albums)
    override val orders: List<Action> = listOf(ORDER_BY_NONE, ORDER_BY_TITLE, ORDER_BY_ARTIST)

    private val Action.toMediaStoreOrder
        get() = when (this) {
            ORDER_BY_TITLE -> MediaStore.Audio.Albums.ALBUM
            ORDER_BY_ARTIST -> MediaStore.Audio.Albums.ARTIST
            else -> MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
        }

    override suspend fun fetch(filter: Filter, query: String?): Mapped<Album> {
        val (ascending, order) = filter
        val albums = provider.fetchAlbums(query, order.toMediaStoreOrder, ascending)
        return when (order) {
            ORDER_BY_TITLE -> albums.groupBy { it.firstTitleChar }
            ORDER_BY_ARTIST -> albums.groupBy { it.artist }
            else -> albums.groupBy { "" }
        }
    }
}