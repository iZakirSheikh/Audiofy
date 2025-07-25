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

package com.zs.audiofy.videos

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.compose.directory.FilesViewState
import com.zs.core.store.models.Audio
import com.zs.core.store.models.Video

private const val PARAM_ARG = "param_parent"

object RouteVideos: Route {
    override val route: String = "$domain/{${PARAM_ARG}}"

    operator fun invoke(parent: String) =
        "$domain/${Uri.encode(parent)}"
}

operator fun SavedStateHandle.get(route: RouteVideos) =
    get<String>(PARAM_ARG).takeIf { it?.contains(PARAM_ARG) == false }

interface VideosViewState: FilesViewState<Video> {
    fun onPerformAction(value: Action, resolver: Activity, focused: Video?= null)

    fun toggleLiked(value: Video)
}