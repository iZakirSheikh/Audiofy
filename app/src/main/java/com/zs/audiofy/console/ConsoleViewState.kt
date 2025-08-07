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

import android.net.Uri
import com.zs.audiofy.common.Route
import com.zs.core.playback.NowPlaying
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

object RouteConsole: Route {

    const val SHARED_ELEMENT_PLAYING_BARS = "playing_bars"
    const val SHARED_ELEMENT_CONTROLS = "controls"
    const val SHARED_ELEMENT_BACKGROUND = "background"
    const val SHARED_ELEMENT_ARTWORK = "artwork"
    const val SHARED_ELEMENT_TITLE = "title"
    const val SHARED_ELEMENT_SUBTITLE = "subtitle"
}

interface ConsoleViewState {

    val state: StateFlow<NowPlaying?>
    val isLiked: Boolean

    fun skipToNext()
    fun skipToPrev()
    fun togglePlay()
    fun seekTo(pct: Float)
    fun shuffle(enable: Boolean)
    fun cycleRepeatMode()
    fun addToLiked(uri: Uri)

}