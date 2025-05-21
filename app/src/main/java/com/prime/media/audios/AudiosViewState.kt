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

package com.prime.media.audios

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.prime.media.common.Route

private const val ARG_SOURCE = "arg_source"
private const val ARG_KEY = "arg_key"

object RouteAudios : Route {
    const val SOURCE_ALL = "source_all"
    const val SOURCE_FOLDER = "source_folder"
    const val SOURCE_ARTIST = "source_artist"
    const val SOURCE_ALBUM = "source_album"
    const val SOURCE_GENRE = "source_genre"

    override val route: String =
        "$domain/{$ARG_SOURCE}/{$ARG_KEY}"


    operator fun invoke(source: String, arg: String) =
        "$domain/$source/${Uri.encode(arg)}"
}

operator fun SavedStateHandle.get(key: RouteAudios) =
    (get<String>(ARG_SOURCE) ?: RouteAudios.SOURCE_ALL) to get<String>(ARG_KEY)

interface AudiosViewState