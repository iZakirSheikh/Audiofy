/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.editor

import android.net.Uri
import com.zs.audiofy.common.Route

private const val PARAM_SOURCE = "param_source"

object RouteEditor: Route {

    override val route: String = "$domain/{${PARAM_SOURCE}}"

    /** Navigates to files of folder identified by [source] providing args*/
    operator fun invoke(path: String): String = "$domain/${Uri.encode(path)}"
}

interface EditorViewState {}