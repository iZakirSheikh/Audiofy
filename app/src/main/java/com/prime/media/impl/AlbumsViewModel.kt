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

package com.prime.media.impl

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.prime.media.R
import com.prime.media.common.Action
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.zs.core.store.MediaProvider
import com.zs.core.store.models.Audio.Album
import java.util.Locale

class AlbumsViewModel(provider: MediaProvider) : LocalDirectoryViewModel<Album>(provider) {

    private val ORDER_BY_TITLE = Action(R.string.title, id = MediaStore.Audio.Albums.ALBUM)
    private val ORDER_BY_NONE = Action(R.string.none, id = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER)
    private val ORDER_BY_ARTIST = Action(R.string.artist, id = MediaStore.Audio.Albums.ARTIST)

    override val uri: Uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI


    private val Album.firstTitleChar
        inline get() = title.uppercase(Locale.ROOT)[0].toString()

    override var filter: Filter by mutableStateOf(true to ORDER_BY_TITLE)
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