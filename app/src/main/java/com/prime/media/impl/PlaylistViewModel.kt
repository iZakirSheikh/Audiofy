/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 14-05-2025.
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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Update
import com.prime.media.R
import com.prime.media.common.Action
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.compose.FilterDefaults
import com.prime.media.playlists.PlaylistsViewState
import com.zs.core.db.playlists.Playlist
import com.zs.core.db.playlists.Playlists
import com.zs.preferences.stringPreferenceKey
import kotlinx.coroutines.flow.StateFlow

private val Saver = FilterDefaults.FilterSaver { id ->
    when (id) {
        ORDER_NAME.id -> ORDER_NAME
        ORDER_BY_MODIFIED.id -> ORDER_BY_MODIFIED
        else -> ORDER_NONE
    }
}

private val ORDER_NONE = FilterDefaults.ORDER_NONE
private val ORDER_NAME = Action(R.string.name, Icons.Outlined.Title, id = "sort_name")
private val ORDER_BY_MODIFIED =
    Action(R.string.date_modified, Icons.Outlined.Update, id = "sort_date_modified")

class PlaylistViewModel(playlists: Playlists) : KoinViewModel(), PlaylistsViewState {
    private val filterKey =
        stringPreferenceKey("playlist_filter_key", null, saver = Saver)

    override fun onAction(
        playlist: Playlist,
        action: Action
    ) {
        TODO("Not yet implemented")
    }

    override val title: CharSequence
        get() = TODO("Not yet implemented")
    override val orders: List<Action>
        get() = TODO("Not yet implemented")
    override val data: StateFlow<Mapped<Playlist>?>
        get() = TODO("Not yet implemented")
    override val query: TextFieldState
        get() = TODO("Not yet implemented")

    override fun filter(ascending: Boolean, order: Action) {
        TODO("Not yet implemented")
    }

    override val filter: Filter
        get() = TODO("Not yet implemented")
}