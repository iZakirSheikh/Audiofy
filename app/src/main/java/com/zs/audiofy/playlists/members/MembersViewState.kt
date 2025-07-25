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

package com.zs.audiofy.playlists.members

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.compose.directory.FilesViewState
import com.zs.core.db.playlists.Playlist.Track
import com.zs.core.store.models.Audio


private const val PARAM_PLAYLIST_NAME = "param_name"

object RouteMembers : Route {

    override val route: String = "$domain/{$PARAM_PLAYLIST_NAME}/"

    /** Navigates to files of folder identified by [source] providing args*/
    operator fun invoke(source: String, key: String = ""): String =
        "$domain/${source}/${Uri.encode(key)}"
}

operator fun SavedStateHandle.get(route: RouteMembers) =
    get<String>(PARAM_PLAYLIST_NAME).takeIf { it?.contains(PARAM_PLAYLIST_NAME) == false }

interface MembersViewState: FilesViewState<Track> {

    val showFavButton: Boolean

    fun onPerformAction(value: Action, focused: Track?= null)

    fun toggleLiked(value: Track)
}