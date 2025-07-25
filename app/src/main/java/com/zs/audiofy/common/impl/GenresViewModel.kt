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
import com.zs.core.store.MediaProvider
import com.zs.core.store.models.Audio.Genre
import com.zs.preferences.stringPreferenceKey
import java.util.Locale
// Genres ViewModel.
private val ORDER_BY_TITLE = FilterDefaults.ORDER_BY_TITLE
private val ORDER_BY_NONE = FilterDefaults.ORDER_NONE

class GenresViewModel(provider: MediaProvider) : LocalDirectoryViewModel<Genre>(provider) {



    private val Genre.firstTitleChar
        inline get() = name.uppercase(Locale.ROOT)[0].toString()

    override val uri: Uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
    override fun filter(ascending: Boolean, order: Action) {
        super.filter(ascending, order)
        preferences[filterKey] = ascending to order
    }

    val filterKey =
        stringPreferenceKey(
            "genre_filter_key",
            null,
            FilterDefaults.FilterSaver {
                when (it) {
                    ORDER_BY_TITLE.id -> ORDER_BY_TITLE
                    else -> ORDER_BY_NONE
                }
            }
        )
    override var filter: Filter by mutableStateOf(
        preferences[filterKey] ?: (true to ORDER_BY_TITLE)
    )

    private val Action.toMediaStoreOrder
        get() = when (id) {
            ORDER_BY_TITLE.id -> MediaStore.Audio.Genres.NAME
            else -> MediaStore.Audio.Genres.DEFAULT_SORT_ORDER
        }

    override val title: CharSequence = getText(R.string.genres)
    override val orders: List<Action> = listOf(ORDER_BY_NONE, ORDER_BY_TITLE)

    override suspend fun fetch(filter: Filter, query: String?): Mapped<Genre> {
        val (ascending, order) = filter
        val result = provider.fetchGenres(query, order.toMediaStoreOrder, ascending)
        return when (order) {
            ORDER_BY_TITLE -> result.groupBy { it.firstTitleChar }
            else -> result.groupBy { "" }
        }
    }
}