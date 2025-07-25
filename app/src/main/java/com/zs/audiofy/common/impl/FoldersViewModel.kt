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
import android.text.format.DateUtils
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Filter
import com.zs.audiofy.common.Mapped
import com.zs.audiofy.common.compose.FilterDefaults
import com.zs.audiofy.common.compose.FilterDefaults.FilterSaver
import com.zs.core.common.debounceAfterFirst
import com.zs.audiofy.common.raw
import com.zs.audiofy.folders.FoldersViewState
import com.zs.audiofy.folders.RouteFolders
import com.zs.audiofy.folders.get
import com.zs.compose.foundation.castTo
import com.zs.compose.theme.snackbar.SnackbarResult
import com.zs.core.store.MediaProvider
import com.zs.core.store.models.Folder
import com.zs.preferences.stringPreferenceKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

private val ORDER_BY_DATE_MODIFIED = FilterDefaults.ORDER_BY_DATE_MODIFIED
private val ORDER_BY_TITLE = FilterDefaults.ORDER_BY_TITLE
private val ORDER_BY_NONE = FilterDefaults.ORDER_NONE

// Folders
@OptIn(ExperimentalCoroutinesApi::class)
class FoldersViewModel(
    handle: SavedStateHandle,
    provider: MediaProvider
) : KoinViewModel(), FoldersViewState {

    private val Action.toMediaStoreOrder
        get() = when (this) {
            ORDER_BY_TITLE -> MediaProvider.COLUMN_NAME
            ORDER_BY_DATE_MODIFIED -> MediaProvider.COLUMN_DATE_MODIFIED
            else -> MediaProvider.COLUMN_DATE_MODIFIED
        }

    // FIXME: Might cause crash.
    private val Folder.firstTitleChar
        inline get() = name.uppercase(Locale.ROOT)[0].toString()

    // Deterimine whose folders to load
    override val ofAudios = handle[RouteFolders]
    val observable: Uri =
        if (ofAudios) MediaProvider.EXTERNAL_AUDIO_URI else MediaProvider.EXTERNAL_VIDEO_URI
    private val filterKey =
        stringPreferenceKey(
            if (ofAudios == true) "folders_filter_audios" else "folders_filter_videos",
            null,
            FilterSaver {
                when (it) {
                    ORDER_BY_TITLE.id -> ORDER_BY_TITLE
                    ORDER_BY_DATE_MODIFIED.id -> ORDER_BY_DATE_MODIFIED
                    else -> ORDER_BY_NONE
                }
            }
        )

    override val title: CharSequence =
        getText(if (ofAudios) R.string.scr_audio_folders_title else R.string.scr_video_folders_title)


    override val orders: List<Action> =
        listOf(ORDER_BY_NONE, ORDER_BY_DATE_MODIFIED, ORDER_BY_TITLE)
    override val query: TextFieldState = TextFieldState()
    override var filter: Filter by mutableStateOf(
        preferences[filterKey] ?: (true to ORDER_BY_DATE_MODIFIED)
    )

    override fun filter(ascending: Boolean, order: Action) {
        if (ascending == filter.first && order == filter.second) return
        val newFilter = ascending to order
        preferences[filterKey] = newFilter
        filter = newFilter
    }


    override val data: StateFlow<Mapped<Folder>?> = combine(
        snapshotFlow(query::raw).onStart { emit(null) }.drop(1),
        snapshotFlow(::filter),
        transform = { query, filter -> query to filter }
    )
        // transform it
        .flatMapLatest { (query, filter) ->
            provider.observer(observable).map {
                val (ascending, order) = filter
                val folders =
                    if (ofAudios) provider.fetchAudioFolders(ascending = false) else provider.fetchVideoFolders(
                        ascending = false
                    )
                val result = when (order) {
                    ORDER_BY_NONE -> folders.groupBy { "" }
                    ORDER_BY_TITLE -> folders.sortedBy { it.firstTitleChar }
                        .let { if (ascending) it else it.reversed() }.groupBy { it.firstTitleChar }

                    ORDER_BY_DATE_MODIFIED -> folders.sortedBy { it.lastModified }
                        .let { if (ascending) it else it.reversed() }
                        .groupBy { DateUtils.getRelativeTimeSpanString(it.lastModified).toString() }

                    else -> error("Oops invalid id passed $order")
                }
                // This should be safe
                castTo(result) as Mapped<Folder>
            }
        }
        .debounceAfterFirst(300L)
        // catch any exceptions.
        .catch {
            val report = report(it.message ?: getText(R.string.msg_unknown_error))
            if (report == SnackbarResult.ActionPerformed) analytics.record(it)
        }
        // make sure the flow is released after sometime.
        .stateIn(viewModelScope, WhileSubscribed(), null)

}