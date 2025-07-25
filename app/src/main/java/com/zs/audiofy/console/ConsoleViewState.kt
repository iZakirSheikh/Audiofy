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

package com.zs.audiofy.console

import com.zs.audiofy.common.Route

object RouteConsole: Route {

    const val SHARED_ELEMENT_PLAYING_BARS = "shared_element_playing_bars"
    const val SHARED_ELEMENT_CONTROLS = "shared_element_controls"
    const val SHARED_ELEMENT_BACKGROUND = "shared_element_background"
    const val SHARED_ELEMENT_ARTWORK = "shared_element_artwork"
    const val SHARED_ELEMENT_TITLE = "shared_element_title"
    const val SHARED_ELEMENT_SUBTITLE = "shared_element_subtitle"
}

interface ConsoleViewState