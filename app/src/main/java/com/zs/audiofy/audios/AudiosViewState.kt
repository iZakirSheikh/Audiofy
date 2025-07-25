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

package com.zs.audiofy.audios

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.compose.directory.FilesViewState
import com.zs.core.store.models.Audio

private const val PARAM_ARG = "param_arg"
private const val PARAM_SOURCE = "param_source"

object RouteAudios : Route {
    // Don't change value of these as these are required to remember filter keys.
    const val SOURCE_ALL = "source_all"
    const val SOURCE_FOLDER = "source_folder"
    const val SOURCE_ARTIST = "source_artist"
    const val SOURCE_ALBUM = "source_album"
    const val SOURCE_GENRE = "source_genre"

    override val route: String = "$domain/{$PARAM_SOURCE}/{$PARAM_ARG}"

    /** Navigates to files of folder identified by [source] providing args*/
    operator fun invoke(source: String, key: String = ""): String =
        "$domain/${source}/${Uri.encode(key)}"
}

operator fun SavedStateHandle.get(route: RouteAudios) =
// FixMe: This is a workaround for a potential issue where navigation arguments
//       might not be null as expected when navigating without explicit arguments.
//       Investigate why the arguments are sometimes populated with keys
//       instead of being null. This `takeIf` ensures that we only use the
//       arguments if they don't seem to be the default placeholder keys.
//       A more robust solution would be to understand and fix the root cause
    //       of this behavior in the navigation logic.
    (get<String>(PARAM_SOURCE).takeIf { it?.contains(PARAM_SOURCE) == false }
        ?: RouteAudios.SOURCE_ALL) to get<String>(PARAM_ARG).takeIf { it?.contains(PARAM_ARG) == false }


interface AudiosViewState: FilesViewState<Audio> {

    fun onPerformAction(value: Action, resolver: Activity, focused: Audio?= null)

    fun toggleLiked(value: Audio)
}