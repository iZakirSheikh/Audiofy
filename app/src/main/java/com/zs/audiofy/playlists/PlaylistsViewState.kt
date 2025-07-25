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

package com.zs.audiofy.playlists

import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.compose.directory.DirectoryViewState
import com.zs.core.db.playlists.Playlist

object RoutePlaylists : Route

interface PlaylistsViewState : DirectoryViewState<Playlist> {

    var showEditDialog: Boolean

    fun onAction(action: Action)

    fun create(value: Playlist)

    fun delete(playlist: Playlist)

    fun update(playlist: Playlist)

}