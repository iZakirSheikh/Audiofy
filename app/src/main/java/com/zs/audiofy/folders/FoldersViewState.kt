/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 02-06-2025.
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

package com.zs.audiofy.folders

import androidx.lifecycle.SavedStateHandle
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.compose.directory.DirectoryViewState
import com.zs.audiofy.common.invoke
import com.zs.core.store.models.Folder

object RouteFolders : Route {
    const val PARAM_IS_OF_AUDIOS = "param_is_of_audios"
    override val route: String = "$domain/{${PARAM_IS_OF_AUDIOS}}"

    override fun invoke(): String = invoke(true)
    operator fun invoke(ofAudios: Boolean): String = "$domain/${ofAudios}"
}

operator fun SavedStateHandle.get(handle: RouteFolders) =
    this<Boolean>(handle.PARAM_IS_OF_AUDIOS) == true

interface FoldersViewState : DirectoryViewState<Folder> {
    val ofAudios: Boolean
}