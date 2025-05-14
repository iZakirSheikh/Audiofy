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
import android.text.format.DateUtils
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.prime.media.R
import com.prime.media.common.Action
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.compose.directory.DirectoryViewState
import com.prime.media.common.debounceAfterFirst
import com.prime.media.common.raw
import com.zs.compose.theme.snackbar.SnackbarResult
import com.zs.core.store.MediaProvider
import com.zs.core.store.models.Audio.Album
import com.zs.core.store.models.Audio.Artist
import com.zs.core.store.models.Audio.Genre
import com.zs.core.store.models.Folder
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



class GenresViewModel(provider: MediaProvider) : LocalDirectoryViewModel<Genre>(provider) {

    // Genres ViewModel.
    private val GENRES_ORDER_BY_TITLE = Action(R.string.title, id = MediaStore.Audio.Genres.NAME)
    private val GENRE_ORDER_BY_NONE = Action(R.string.none, id = MediaStore.Audio.Genres._ID)

    private val Genre.firstTitleChar
        inline get() = name.uppercase(Locale.ROOT)[0].toString()

    override val uri: Uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI

    override var filter: Filter by mutableStateOf(true to GENRES_ORDER_BY_TITLE)
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

