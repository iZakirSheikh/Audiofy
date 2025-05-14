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
import com.zs.core.store.models.Audio.Artist
import java.util.Locale

// Artists
class ArtistsViewModel(provider: MediaProvider) : LocalDirectoryViewModel<Artist>(provider) {

    override val uri: Uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI

    private val Artist.firstTitleChar
        inline get() = name.uppercase(Locale.ROOT)[0].toString()
    private val ARTISTS_ORDER_BY_TITLE = Action(R.string.title, id = MediaStore.Audio.Artists.ARTIST)
    private val ARTISTS_ORDER_BY_NONE = Action(R.string.none, id = MediaStore.Audio.Artists._ID)


    override var filter: Filter by mutableStateOf(true to ARTISTS_ORDER_BY_TITLE)
    override val title: CharSequence = getText(R.string.artists)
    override val orders: List<Action> = listOf(ARTISTS_ORDER_BY_NONE, ARTISTS_ORDER_BY_TITLE)

    override suspend fun fetch(filter: Filter, query: String?): Mapped<Artist> {
        val (ascending, order) = filter
        val result = provider.fetchArtists(query, order.id, ascending)
        return when (order) {
            ARTISTS_ORDER_BY_TITLE -> result.groupBy { it.firstTitleChar }
            else -> result.groupBy { "" }
        }
    }
}