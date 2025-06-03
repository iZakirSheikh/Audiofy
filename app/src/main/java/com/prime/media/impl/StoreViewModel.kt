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

package com.prime.media.impl

import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.prime.media.R
import com.prime.media.common.Action
import com.prime.media.common.INFO
import com.prime.media.common.compose.FilterDefaults
import com.prime.media.common.debounceAfterFirst
import com.prime.media.common.raw
import com.zs.core.store.MediaProvider
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class StoreViewModel<T>(
    provider: MediaProvider
): FilesViewModel<T>() {

    private val TAG = "StoreViewModel"

    // Some common orders
   val ORDER_BY_NONE = FilterDefaults.ORDER_NONE
   val ORDER_BY_TITLE = FilterDefaults.ORDER_BY_TITLE
   val ORDER_BY_DATE_MODIFIED = FilterDefaults.ORDER_BY_DATE_MODIFIED
   val ORDER_BY_LENGTH get() = Action(R.string.length, id = "filter_by_length")

    // common actions
    val ACTION_ADD_TO_PLAYLIST = Action(R.string.add_to_playlist, Icons.Outlined.PlaylistAdd)
    val ACTION_PLAY_NEXT = Action(R.string.play_next, Icons.Outlined.SkipNext)
    val ACTION_ADD_TO_QUEUE = Action(R.string.add_to_queue, Icons.AutoMirrored.Outlined.QueueMusic)
    val ACTION_DELETE = Action(R.string.delete, Icons.Default.DeleteOutline)
    val ACTION_SHARE = Action(R.string.share, Icons.Outlined.Share)
    val ACTION_SELECT_ALL = Action(R.string.select_all, Icons.Outlined.SelectAll)
    val ACTION_INFO = Action.INFO

    abstract suspend fun refresh(query: String?, ascending: Boolean, order: Action)

    abstract val contentUri: Uri

    override fun play(from: T?) {
        TODO("Not yet implemented")
    }

    override fun shuffle() {
        TODO("Not yet implemented")
    }

    val flow by lazy {
        combine(
            flow = provider.observer(contentUri),
            flow2 = snapshotFlow(query::raw),
            flow3 = snapshotFlow(::filter),
            transform = { _, query, filter -> Triple(query, filter.first, filter.second) }
        ).debounceAfterFirst(300)
            .onEach() { (query, ascending, order) -> refresh(query, ascending, order) }
            .catch { exception ->
                Log.d(TAG, "provider: ${exception.stackTraceToString()}")
                val action = report(exception.message ?: getText(R.string.msg_unknown_error))
            }
    }
}